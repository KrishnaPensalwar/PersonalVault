package com.example.sample.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.security.data.AppLockRepository
import com.example.sample.security.data.UnlockMethod
import javax.crypto.Cipher
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
            VaultSession.clear()
            evaluateLockState()
        }
    }

    fun biometricUnlockCipher(): Cipher? = repository.biometricUnlockCipher()

    fun biometricEnrollCipher(): Cipher? = repository.biometricEnrollCipher()

    fun onBiometricUnlock(cipher: Cipher?) {
        if (cipher != null && repository.completeBiometricUnlock(cipher)) {
            unlock()
        } else if (repository.unlockMethod() == UnlockMethod.DEVICE) {
            _lockState.update { LockScreenState.Biometric }
        } else {
            showPinEntry()
        }
    }

    fun onBiometricEnroll(cipher: Cipher?) {
        val saved = cipher != null && repository.completeBiometricEnroll(cipher)
        if (repository.unlockMethod() == UnlockMethod.DEVICE && !saved) {
            VaultSession.clear()
            repository.setUnlockMethod(UnlockMethod.UNSET)
            _lockState.update { LockScreenState.ChooseUnlock("Device lock was not saved. Pick an unlock method.") }
            return
        }
        unlock()
    }

    fun onBiometricFailed(message: String) {
        if (_lockState.value is LockScreenState.BiometricEnroll) {
            if (repository.unlockMethod() == UnlockMethod.DEVICE && !repository.hasBiometricWrap()) {
                VaultSession.clear()
                repository.setUnlockMethod(UnlockMethod.UNSET)
                _lockState.update { LockScreenState.ChooseUnlock("Device lock was cancelled.") }
            } else {
                unlock()
            }
            return
        }
        viewModelScope.launch { _events.emit(LockScreenEvent.Message(message)) }
        if (repository.unlockMethod() == UnlockMethod.PIN && repository.isPinSetupComplete()) showPinEntry()
    }

    fun onChoosePin() {
        repository.setUnlockMethod(UnlockMethod.PIN)
        _lockState.update { LockScreenState.PinSetup() }
    }

    fun onChooseDevice() {
        if (!biometricChecker.isAvailable()) {
            _lockState.update { LockScreenState.ChooseUnlock("Set a screen lock or fingerprint on this phone first.") }
            return
        }
        if (!repository.prepareDeviceLock()) {
            VaultSession.clear()
            repository.setUnlockMethod(UnlockMethod.UNSET)
            _lockState.update { LockScreenState.ChooseUnlock("This phone could not store a device-lock key.") }
            return
        }
        _lockState.update { LockScreenState.BiometricEnroll }
    }

    fun onChooseNone() {
        if (repository.enableNoAuthentication()) {
            unlock()
        } else {
            _lockState.update { LockScreenState.ChooseUnlock("Could not store the vault key without a lock.") }
        }
    }

    fun onPinSetupSubmit(pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        val error = when {
            pin.length != 6 || pin.any { !it.isDigit() } -> "PIN must be 6 digits"
            pin != confirmPin -> "PINs do not match"
            recoveryAnswer.isBlank() -> "Answer is required"
            else -> null
        }
        if (error != null) {
            _lockState.update { LockScreenState.PinSetup(error) }
            return
        }
        repository.savePin(pin, recoveryQuestionId, recoveryAnswer)
        afterSecretInstalled()
    }

    fun onPinEntrySubmit(pin: String) {
        val lockedUntil = repository.pinLockedUntil()
        if (lockedUntil > System.currentTimeMillis()) {
            _lockState.update {
                LockScreenState.PinEntry(
                    error = "Too many attempts. Try again in ${minutesLeft(lockedUntil)}m",
                    failedAttempts = repository.failedPinAttempts(),
                    lockedUntilEpochMs = lockedUntil,
                )
            }
            return
        }
        if (repository.verifyPin(pin)) {
            repository.clearPinFailures()
            afterSecretInstalled()
        } else {
            val attempts = repository.registerFailedPin()
            val until = repository.pinLockedUntil()
            val error = if (until > System.currentTimeMillis()) {
                "Too many attempts. Locked for 10 minutes"
            } else {
                "Incorrect PIN"
            }
            _lockState.update {
                LockScreenState.PinEntry(error, attempts, until)
            }
        }
    }

    fun onForgotPinClicked() {
        val question = repository.getRecoveryQuestionId()?.let(RecoveryQuestion::fromId) ?: return
        _lockState.update { LockScreenState.RecoveryPrompt(question) }
    }

    fun onRecoveryAnswerSubmit(answer: String) {
        val current = _lockState.value
        if (current !is LockScreenState.RecoveryPrompt) return
        val lockedUntil = repository.recoveryLockedUntil()
        if (lockedUntil > System.currentTimeMillis()) {
            _lockState.update {
                current.copy(error = "Too many attempts. Try again in ${minutesLeft(lockedUntil)}m")
            }
            return
        }
        if (repository.verifyRecoveryAnswer(answer)) {
            repository.clearRecoveryFailures()
            _lockState.update { LockScreenState.SetNewPin() }
        } else {
            repository.registerFailedRecovery()
            val until = repository.recoveryLockedUntil()
            val error = if (until > System.currentTimeMillis()) {
                "Too many attempts. Locked for 10 minutes"
            } else {
                "Incorrect answer"
            }
            _lockState.update { current.copy(error = error) }
        }
    }

    fun onSetNewPinSubmit(pin: String, confirmPin: String) {
        val error = when {
            pin.length != 6 || pin.any { !it.isDigit() } -> "PIN must be 6 digits"
            pin != confirmPin -> "PINs do not match"
            else -> null
        }
        if (error != null) {
            _lockState.update { LockScreenState.SetNewPin(error) }
            return
        }
        if (!repository.resetPin(pin)) {
            _lockState.update { LockScreenState.SetNewPin("Could not protect the new PIN") }
            return
        }
        afterSecretInstalled()
    }

    fun onBackToPinEntry() {
        _lockState.update {
            LockScreenState.PinEntry(
                failedAttempts = repository.failedPinAttempts(),
                lockedUntilEpochMs = repository.pinLockedUntil(),
            )
        }
    }

    private fun minutesLeft(untilEpochMs: Long): Long {
        val left = untilEpochMs - System.currentTimeMillis()
        return (left / 60_000L).coerceAtLeast(1L)
    }

    private fun evaluateLockState() {
        _lockState.update {
            when (repository.unlockMethod()) {
                UnlockMethod.NONE ->
                    if (repository.unlockWithOpenKey()) LockScreenState.Unlocked
                    else LockScreenState.ChooseUnlock("Could not open the vault.")
                UnlockMethod.DEVICE ->
                    if (repository.hasBiometricWrap() && biometricChecker.isAvailable()) LockScreenState.Biometric
                    else LockScreenState.ChooseUnlock()
                UnlockMethod.PIN ->
                    if (!repository.isPinSetupComplete()) LockScreenState.PinSetup()
                    else if (biometricChecker.isAvailable() && repository.hasBiometricWrap()) LockScreenState.Biometric
                    else LockScreenState.PinEntry(
                        failedAttempts = repository.failedPinAttempts(),
                        lockedUntilEpochMs = repository.pinLockedUntil(),
                    )
                UnlockMethod.UNSET -> LockScreenState.ChooseUnlock()
            }
        }
    }

    private fun afterSecretInstalled() {
        if (biometricChecker.isAvailable() && !repository.hasBiometricWrap() && repository.biometricEnrollCipher() != null) {
            _lockState.update { LockScreenState.BiometricEnroll }
        } else {
            unlock()
        }
    }

    private fun showPinEntry() {
        _lockState.update {
            LockScreenState.PinEntry(
                failedAttempts = repository.failedPinAttempts(),
                lockedUntilEpochMs = repository.pinLockedUntil(),
            )
        }
    }

    private fun unlock() {
        _lockState.update { LockScreenState.Unlocked }
    }
}
