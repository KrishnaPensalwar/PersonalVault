package com.example.sample.security.data

import javax.crypto.Cipher

/**
 * PIN and recovery passphrase wrap the vault data key. A correct check installs that key
 * in [com.example.sample.security.VaultSession]. A wrong check does not.
 */
interface AppLockRepository {

    fun isPinSetupComplete(): Boolean

    /** Epoch millis of the last PIN set or change. 0 if this install never recorded one. */
    fun pinChangedAt(): Long

    fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String)

    /** Unwraps the data key with [pin]. Returns false for a wrong PIN or a damaged wrap. */
    fun verifyPin(pin: String): Boolean

    fun getRecoveryQuestionId(): Int?

    /** Unwraps the data key with [answer]. Returns false for a wrong passphrase. */
    fun verifyRecoveryAnswer(answer: String): Boolean

    /** Re-wraps the current data key with [newPin]. Fails if the vault key is not in memory. */
    fun resetPin(newPin: String): Boolean

    fun failedPinAttempts(): Int

    fun pinLockedUntil(): Long

    fun registerFailedPin(): Int

    fun clearPinFailures()

    fun failedRecoveryAttempts(): Int

    fun recoveryLockedUntil(): Long

    fun registerFailedRecovery(): Int

    fun clearRecoveryFailures()

    fun hasBiometricWrap(): Boolean

    /** Cipher the biometric prompt must authorize before the data key can be unwrapped. */
    fun biometricUnlockCipher(): Cipher?

    fun completeBiometricUnlock(cipher: Cipher): Boolean

    /** Cipher the biometric prompt must authorize before the data key can be wrapped. */
    fun biometricEnrollCipher(): Cipher?

    fun completeBiometricEnroll(cipher: Cipher): Boolean

    fun unlockMethod(): UnlockMethod

    fun setUnlockMethod(method: UnlockMethod)

    /** Opens the vault with the keystore wrap that does not ask for authentication. */
    fun unlockWithOpenKey(): Boolean

    /**
     * Wraps the in-memory data key, or a new one, with a Keystore key that does not require
     * authentication. Removes the PIN and recovery wraps.
     */
    fun enableNoAuthentication(): Boolean

    /** Keeps the current data key, or creates one, and records device-lock mode. */
    fun prepareDeviceLock(): Boolean

    /** Wraps the current data key with a new PIN. Used when switching back to a PIN. */
    fun wrapCurrentKeyWithPin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String): Boolean
}

enum class UnlockMethod { UNSET, PIN, DEVICE, NONE }
