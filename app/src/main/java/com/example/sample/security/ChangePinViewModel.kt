package com.example.sample.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.security.data.AppLockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChangePinUiState(
    val error: String? = null,
    val saved: Boolean = false,
    val saving: Boolean = false,
)

class ChangePinViewModel(
    private val repository: AppLockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePinUiState())
    val state: StateFlow<ChangePinUiState> = _state.asStateFlow()

    fun onSubmit(currentPin: String, newPin: String, confirmPin: String) {
        if (_state.value.saving) return
        val lockedUntil = repository.pinLockedUntil()
        if (lockedUntil > System.currentTimeMillis()) {
            _state.update { it.copy(error = "Too many attempts. Try again in ${minutesLeft(lockedUntil)}m") }
            return
        }
        val fastError = when {
            currentPin.length != 6 || newPin.length != 6 -> "PIN must be 6 digits"
            currentPin.any { !it.isDigit() } || newPin.any { !it.isDigit() } -> "PIN must be 6 digits"
            newPin != confirmPin -> "PINs do not match"
            newPin == currentPin -> "Choose a different PIN"
            else -> null
        }
        if (fastError != null) {
            _state.update { ChangePinUiState(error = fastError) }
            return
        }
        _state.update { ChangePinUiState(saving = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val error = when {
                !repository.verifyPin(currentPin) -> {
                    repository.registerFailedPin()
                    val until = repository.pinLockedUntil()
                    if (until > System.currentTimeMillis()) "Too many attempts. Locked for 10 minutes" else "Incorrect PIN"
                }
                !repository.resetPin(newPin) -> "Could not protect the new PIN"
                else -> null
            }
            if (error == null) repository.clearPinFailures()
            _state.update { ChangePinUiState(error = error, saved = error == null) }
        }
    }

    fun consumeSaved() {
        _state.update { it.copy(saved = false) }
    }

    private fun minutesLeft(untilEpochMs: Long): Long {
        val left = untilEpochMs - System.currentTimeMillis()
        return (left / 60_000L).coerceAtLeast(1L)
    }
}
