package com.example.sample.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.security.data.AppLockRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppLockViewModel(
    private val repository: AppLockRepository,
    private val biometricChecker: BiometricAvailabilityChecker,
) : ViewModel() {

    private val _lockState = MutableStateFlow<LockScreenState>(LockScreenState.Checking)
    val lockState: StateFlow<LockScreenState> = _lockState.asStateFlow()

    private val _events = MutableSharedFlow<LockScreenEvent>()
    val events = _events.asSharedFlow()

    init {
        evaluateLockState()
    }

    /** Called when the hosting lifecycle re-enters the foreground. */
    fun onAppForegrounded() {
        if (AppBackgroundTracker.consumeShouldLock()) {
            evaluateLockState()
        }
    }

    fun onBiometricSuccess() = unlock()

    fun onBiometricFailed(message: String) {
        viewModelScope.launch { _events.emit(LockScreenEvent.Message(message)) }
    }

    fun onPinSetupSubmit(pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        val error = when {
            pin.length != 4 || pin.any { !it.isDigit() } -> "PIN must be 4 digits"
            pin != confirmPin -> "PINs do not match"
            recoveryAnswer.isBlank() -> "Answer is required"
            else -> null
        }
        if (error != null) {
            _lockState.update { LockScreenState.PinSetup(error) }
            return
        }
        repository.savePin(pin, recoveryQuestionId, recoveryAnswer)
        unlock()
    }

    fun onPinEntrySubmit(pin: String) {
        if (repository.verifyPin(pin)) {
            unlock()
        } else {
            _lockState.update { LockScreenState.PinEntry("Incorrect PIN") }
        }
    }

    fun onForgotPinClicked() {
        val question = repository.getRecoveryQuestionId()?.let(RecoveryQuestion::fromId) ?: return
        _lockState.update { LockScreenState.RecoveryPrompt(question) }
    }

    fun onRecoveryAnswerSubmit(answer: String) {
        val current = _lockState.value
        if (current !is LockScreenState.RecoveryPrompt) return
        if (repository.verifyRecoveryAnswer(answer)) {
            _lockState.update { LockScreenState.SetNewPin() }
        } else {
            _lockState.update { current.copy(error = "Incorrect answer") }
        }
    }

    fun onSetNewPinSubmit(pin: String, confirmPin: String) {
        val error = when {
            pin.length != 4 || pin.any { !it.isDigit() } -> "PIN must be 4 digits"
            pin != confirmPin -> "PINs do not match"
            else -> null
        }
        if (error != null) {
            _lockState.update { LockScreenState.SetNewPin(error) }
            return
        }
        repository.resetPin(pin)
        unlock()
    }

    fun onBackToPinEntry() {
        _lockState.update { LockScreenState.PinEntry() }
    }

    private fun evaluateLockState() {
        _lockState.update {
            when {
                biometricChecker.isAvailable() -> LockScreenState.Biometric
                !repository.isPinSetupComplete() -> LockScreenState.PinSetup()
                else -> LockScreenState.PinEntry()
            }
        }
    }

    private fun unlock() {
        _lockState.update { LockScreenState.Unlocked }
    }
}
