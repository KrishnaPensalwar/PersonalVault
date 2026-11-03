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

    fun failedPinAttempts(): Int

    /** Epoch millis until PIN entry is allowed again. 0 when not locked. */
    fun pinLockedUntil(): Long

    /** Records a wrong PIN. Locks for 10 minutes after 5 failures. Returns the new attempt count. */
    fun registerFailedPin(): Int

    fun clearPinFailures()
}
