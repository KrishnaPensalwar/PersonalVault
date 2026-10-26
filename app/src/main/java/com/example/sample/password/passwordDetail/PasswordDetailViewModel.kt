package com.example.sample.password.passwordDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.password.data.PasswordRepository
import com.example.sample.password.domain.Password
import com.example.sample.password.domain.toDomain
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class PasswordDetailViewModel(
    private val repository: PasswordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordDetailUiState())
    val uiState: StateFlow<PasswordDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PasswordDetailEvent>()
    val events = _events.asSharedFlow()

    fun loadPasswordDetail(id: String) {
        _uiState.update { PasswordDetailUiState(isLoading = true) }
        viewModelScope.launch {
            runCatching {
                val entity = repository.getPasswordById(id)
                _uiState.update {
                    PasswordDetailUiState(
                        password = entity?.toDomain(),
                        isLoading = false,
                        error = if (entity == null) "Password not found" else null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    PasswordDetailUiState(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load",
                    )
                }
            }
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onToggleFavorite(password: Password) {
        viewModelScope.launch {
            runCatching {
                repository.toggleFavorite(password.id, !password.isFavorite)
                val updatedEntity = repository.getPasswordById(password.id)
                _uiState.update { it.copy(password = updatedEntity?.toDomain()) }
            }.onFailure { throwable ->
                _events.emit(PasswordDetailEvent.Error(throwable.message ?: "Failed to update favourite"))
            }
        }
    }

    fun onDeletePassword(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.deletePasswordById(id)
                _events.emit(PasswordDetailEvent.DeletedSuccessfully)
            }.onFailure { throwable ->
                _events.emit(PasswordDetailEvent.Error(throwable.message ?: "Failed to delete"))
            }
        }
    }
}
