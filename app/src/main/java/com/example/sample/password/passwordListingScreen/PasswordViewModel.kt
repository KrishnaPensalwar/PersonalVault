package com.example.sample.password.passwordListingScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.password.data.PasswordRepository
import com.example.sample.password.domain.Password
import com.example.sample.password.domain.toDomain
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
class PasswordViewModel(
    private val repository: PasswordRepository,
) : ViewModel() {

    // ── Search & filter ───────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(PasswordListFilter.All)
    val filter: StateFlow<PasswordListFilter> = _filter.asStateFlow()

    // ── List state ────────────────────────────────────────────────────────────

    val listUiState: StateFlow<PasswordListUiState> =
        combine(
            repository.getAllPasswords(),
            _searchQuery.debounce(300.milliseconds).distinctUntilChanged(),
            _filter,
        ) { entities, query, filter ->
            val all = entities.toDomain()
            val q = query.trim()
            val searched = if (q.isEmpty()) {
                all
            } else {
                all.filter {
                    it.title.contains(q, ignoreCase = true) ||
                        it.username.contains(q, ignoreCase = true) ||
                        it.websiteUrl.contains(q, ignoreCase = true)
                }
            }
            val shown = when (filter) {
                PasswordListFilter.All -> searched
                PasswordListFilter.Favourites -> searched.filter { it.isFavorite }
                PasswordListFilter.Work -> searched.filter { it.category.equals("Work", true) }
                PasswordListFilter.Personal -> searched.filter { it.category.equals("Personal", true) }
            }
            PasswordListUiState.Success(
                passwords = shown,
                totalCount = all.size,
                favouriteCount = all.count { it.isFavorite },
                workCount = all.count { it.category.equals("Work", true) },
                personalCount = all.count { it.category.equals("Personal", true) },
            ) as PasswordListUiState
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

    fun onFilterSelected(filter: PasswordListFilter) {
        _filter.value = filter
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
