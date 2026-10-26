package com.example.sample.password

import com.example.sample.password.domain.PasswordCategories

data class PasswordFormState(
    val id: String = "",
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val websiteUrl: String = "",
    val notes: String = "",
    val category: String = PasswordCategories[8], // "Other"
    val isFavorite: Boolean = false,
    // ── Validation errors ────────────────────────────────────────────────────
    val titleError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    // ── Operation flags ──────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isEditMode: Boolean = false,
) {
    val isValid: Boolean
        get() = titleError == null && usernameError == null && passwordError == null
                && title.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

sealed interface PasswordFormEvent {
    data object SavedSuccessfully : PasswordFormEvent
    data class Error(val message: String) : PasswordFormEvent
}