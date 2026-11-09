package com.example.sample.password.passwordListingScreen

import com.example.sample.password.domain.Password

// ── UI State ─────────────────────────────────────────────────────────────────

sealed interface PasswordListFilter {
    data object All : PasswordListFilter
    data object Favourites : PasswordListFilter
    data class Category(val name: String) : PasswordListFilter
}

data class CategoryCount(
    val name: String,
    val count: Int,
)

sealed interface PasswordListUiState {
    data object Loading : PasswordListUiState
    data class Success(
        val passwords: List<Password>,
        val totalCount: Int,
        val favouriteCount: Int,
        val categoryCounts: List<CategoryCount>,
    ) : PasswordListUiState
    data class Error(val message: String) : PasswordListUiState
}

// ── One-shot events ───────────────────────────────────────────────────────────

sealed interface PasswordEvent {
    data object DeletedSuccessfully : PasswordEvent
    data class Error(val message: String) : PasswordEvent
}
