package com.example.sample.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.password.data.PasswordRepository
import com.example.sample.password.domain.Password
import com.example.sample.password.domain.toDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PasswordViewModel(
    private val repository: PasswordRepository,
) : ViewModel() {

    // ── Search & filter ───────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    // ── List state ────────────────────────────────────────────────────────────

    val listUiState: StateFlow<PasswordListUiState> =
        combine(
            _searchQuery
                .debounce(400.milliseconds)
                .distinctUntilChanged(),
            _showFavoritesOnly,
        ) { query, favOnly -> query to favOnly }
            .flatMapLatest { (query, favOnly) ->
                val source = when {
                    favOnly -> repository.getFavoritePasswords()
                    query.isBlank() -> repository.getAllPasswords()
                    else -> repository.searchPasswords(query)
                }
                source.map { entities ->
                    PasswordListUiState.Success(entities.toDomain()) as PasswordListUiState
                }
            }
            .catch { throwable ->
                emit(PasswordListUiState.Error(throwable.message ?: "Failed to load passwords"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PasswordListUiState.Loading,
            )

    // ── One-shot events ───────────────────────────────────────────────────────

    private val _events = MutableSharedFlow<PasswordEvent>()
    val events = _events.asSharedFlow()

    // ── Actions: List ─────────────────────────────────────────────────────────

    fun onSearchQueryChanged(query: String) {
        _searchQuery.update { query }
    }

    fun onToggleFavoritesFilter() {
        _showFavoritesOnly.update { !it }
    }

    fun onToggleFavorite(password: Password) {
        viewModelScope.launch {
            runCatching {
                repository.toggleFavorite(password.id, !password.isFavorite)
            }.onFailure {
                _events.emit(PasswordEvent.Error(it.message ?: "Failed to update favourite"))
            }
        }
    }

    fun onDeletePassword(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.deletePasswordById(id)
                _events.emit(PasswordEvent.DeletedSuccessfully)
            }.onFailure {
                _events.emit(PasswordEvent.Error(it.message ?: "Failed to delete"))
            }
        }
    }
}
