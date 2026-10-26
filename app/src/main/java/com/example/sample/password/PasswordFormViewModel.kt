package com.example.sample.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.password.data.PasswordRepository
import com.example.sample.password.domain.Password
import com.example.sample.password.domain.toDomain
import com.example.sample.password.domain.toEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID



class PasswordFormViewModel(
    private val repository: PasswordRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(PasswordFormState())
    val formState: StateFlow<PasswordFormState> = _formState.asStateFlow()

    private val _events = MutableSharedFlow<PasswordFormEvent>()
    val events = _events.asSharedFlow()

    /** Prepare the form for adding a new password. */
    fun initAddForm() {
        _formState.update { PasswordFormState() }
    }

    /** Load an existing password into the form for editing. */
    fun initEditForm(passwordId: String) {
        viewModelScope.launch {
            val entity = repository.getPasswordById(passwordId) ?: return@launch
            val domain = entity.toDomain()
            _formState.update {
                PasswordFormState(
                    id = domain.id,
                    title = domain.title,
                    username = domain.username,
                    password = domain.password,
                    websiteUrl = domain.websiteUrl,
                    notes = domain.notes,
                    category = domain.category,
                    isFavorite = domain.isFavorite,
                    isEditMode = true,
                )
            }
        }
    }

    fun onTitleChanged(value: String) {
        _formState.update {
            it.copy(title = value, titleError = if (value.isBlank()) "Title is required" else null)
        }
    }

    fun onUsernameChanged(value: String) {
        _formState.update {
            it.copy(
                username = value,
                usernameError = if (value.isBlank()) "Username is required" else null,
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _formState.update {
            it.copy(
                password = value,
                passwordError = if (value.isBlank()) "Password is required" else null,
            )
        }
    }

    fun onWebsiteUrlChanged(value: String) = _formState.update { it.copy(websiteUrl = value) }
    fun onNotesChanged(value: String) = _formState.update { it.copy(notes = value) }
    fun onCategoryChanged(value: String) = _formState.update { it.copy(category = value) }
    fun onFavoriteToggled() = _formState.update { it.copy(isFavorite = !it.isFavorite) }

    /** Validate and persist the form. */
    fun onSavePassword() {
        val state = _formState.value

        // Validate
        val updated = state.copy(
            titleError = if (state.title.isBlank()) "Title is required" else null,
            usernameError = if (state.username.isBlank()) "Username is required" else null,
            passwordError = if (state.password.isBlank()) "Password is required" else null,
        )
        _formState.update { updated }
        if (!updated.isValid) return

        _formState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val domain = Password(
                    id = if (state.isEditMode) state.id else UUID.randomUUID().toString(),
                    title = state.title.trim(),
                    username = state.username.trim(),
                    password = state.password,
                    websiteUrl = state.websiteUrl.trim(),
                    notes = state.notes.trim(),
                    category = state.category,
                    isFavorite = state.isFavorite,
                    createdAt = if (state.isEditMode) 0L else now,
                    updatedAt = now,
                )
                if (state.isEditMode) {
                    val existing = repository.getPasswordById(state.id)
                    repository.updatePassword(
                        domain.copy(createdAt = existing?.createdAt ?: now).toEntity()
                    )
                } else {
                    repository.savePassword(domain.copy(createdAt = now).toEntity())
                }
                _events.emit(PasswordFormEvent.SavedSuccessfully)
                _formState.update { PasswordFormState() }
            }.onFailure { throwable ->
                _events.emit(PasswordFormEvent.Error(throwable.message ?: "Failed to save"))
            }.also {
                _formState.update { it.copy(isSaving = false) }
            }
        }
    }
}
