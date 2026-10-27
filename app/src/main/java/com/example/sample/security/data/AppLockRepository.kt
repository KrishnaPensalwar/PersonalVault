package com.example.sample.security.data

/**
 * Storage for the app-managed 4-digit PIN and its recovery question/answer.
 * Only used when the device itself has no biometric or device credential
 * enrolled — see [com.example.sample.security.BiometricAvailabilityChecker].
 */
interface AppLockRepository {

    fun isPinSetupComplete(): Boolean

    fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String)

    fun verifyPin(pin: String): Boolean

    /** Null until [savePin] has been called once. */
    fun getRecoveryQuestionId(): Int?

    fun verifyRecoveryAnswer(answer: String): Boolean

    fun resetPin(newPin: String)
}
