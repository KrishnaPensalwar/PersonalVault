package com.example.sample.password.passwordDetail

import com.example.sample.password.domain.Password

data class PasswordDetailUiState(
    val password: Password? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val passwordVisible: Boolean = false,
)

sealed interface PasswordDetailEvent {
    data object DeletedSuccessfully : PasswordDetailEvent
    data class Error(val message: String) : PasswordDetailEvent
}
