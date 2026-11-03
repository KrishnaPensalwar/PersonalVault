package com.example.sample.password.passwordListingScreen

import com.example.sample.password.domain.Password

// ── UI State ─────────────────────────────────────────────────────────────────

enum class PasswordListFilter { All, Favourites, Work, Personal }

sealed interface PasswordListUiState {
    data object Loading : PasswordListUiState
    data class Success(
        val passwords: List<Password>,
        val totalCount: Int,
        val favouriteCount: Int,
        val workCount: Int,
        val personalCount: Int,
    ) : PasswordListUiState
    data class Error(val message: String) : PasswordListUiState
}

// ── One-shot events ───────────────────────────────────────────────────────────

sealed interface PasswordEvent {
    data object DeletedSuccessfully : PasswordEvent
    data class Error(val message: String) : PasswordEvent
}
