package com.example.sample.security

sealed interface LockScreenState {
    data object Checking : LockScreenState
    data object Biometric : LockScreenState
    data class PinEntry(val error: String? = null) : LockScreenState
    data class PinSetup(val error: String? = null) : LockScreenState
    /** Named `RecoveryPrompt`, not `RecoveryQuestion`, to avoid colliding with the top-level [com.example.sample.security.RecoveryQuestion] enum it wraps. */
    data class RecoveryPrompt(val question: RecoveryQuestion, val error: String? = null) : LockScreenState
    data class SetNewPin(val error: String? = null) : LockScreenState
    data object Unlocked : LockScreenState
}

sealed interface LockScreenEvent {
    data class Message(val text: String) : LockScreenEvent
}
