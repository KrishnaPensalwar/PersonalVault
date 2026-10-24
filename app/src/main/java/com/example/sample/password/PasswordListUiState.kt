package com.example.sample.password

import com.example.sample.password.domain.Password

// ── UI State ─────────────────────────────────────────────────────────────────

sealed interface PasswordListUiState {
    data object Loading : PasswordListUiState
    data class Success(val passwords: List<Password>) : PasswordListUiState
    data class Error(val message: String) : PasswordListUiState
}

// ── One-shot events ───────────────────────────────────────────────────────────

sealed interface PasswordEvent {
    data object DeletedSuccessfully : PasswordEvent
    data class Error(val message: String) : PasswordEvent
}
