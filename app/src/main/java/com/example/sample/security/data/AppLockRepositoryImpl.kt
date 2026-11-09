package com.example.sample.security.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.sample.security.AndroidDekKeystore
import com.example.sample.security.PinHasher
import com.example.sample.security.VaultCrypto
import com.example.sample.security.VaultSession
import com.example.sample.security.constantTimeEquals
import com.example.sample.security.data.UnlockMethod
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher

class AppLockRepositoryImpl(
    private val prefs: SharedPreferences,
    private val keystore: AndroidDekKeystore? = null,
    private val onKeyInstalled: () -> Unit = {},
) : AppLockRepository {

    override fun isPinSetupComplete(): Boolean =
        prefs.contains(KEY_PIN_WRAP) || prefs.contains(KEY_PIN_HASH)

    override fun pinChangedAt(): Long = prefs.getLong(KEY_PIN_CHANGED_AT, 0L)

    override fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        val dek = VaultCrypto.randomDek()
        val pinSalt = VaultCrypto.randomSalt()
        val answerSalt = VaultCrypto.randomSalt()
        prefs.edit {
            putString(KEY_PIN_WRAP, VaultCrypto.wrap(dek, pin.toCharArray(), pinSalt))
            putString(KEY_PIN_SALT, encode(pinSalt))
            putInt(KEY_PIN_ITERATIONS, VaultCrypto.newKeyIterations)
            putInt(KEY_RECOVERY_QUESTION_ID, recoveryQuestionId)
            putString(KEY_RECOVERY_WRAP, VaultCrypto.wrap(dek, recoveryAnswer.trim().toCharArray(), answerSalt))
            putString(KEY_RECOVERY_SALT, encode(answerSalt))
            putInt(KEY_RECOVERY_ITERATIONS, VaultCrypto.newKeyIterations)
            remove(KEY_PIN_HASH)
            remove(KEY_RECOVERY_HASH)
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKED_UNTIL, 0L)
            putInt(KEY_RECOVERY_FAILED, 0)
            putLong(KEY_RECOVERY_LOCKED_UNTIL, 0L)
            putLong(KEY_PIN_CHANGED_AT, System.currentTimeMillis())
        }
        install(dek)
    }

    override fun verifyPin(pin: String): Boolean {
        val wrap = prefs.getString(KEY_PIN_WRAP, null)
        val dek = if (wrap != null) {
            val salt = decode(prefs.getString(KEY_PIN_SALT, null) ?: return false)
            VaultCrypto.unwrap(wrap, pin.toCharArray(), salt, prefs.getInt(KEY_PIN_ITERATIONS, VaultCrypto.newKeyIterations))
        } else if (legacyPinMatches(pin)) {
            val created = VaultCrypto.randomDek()
            val salt = VaultCrypto.randomSalt()
            prefs.edit {
                putString(KEY_PIN_WRAP, VaultCrypto.wrap(created, pin.toCharArray(), salt))
                putString(KEY_PIN_SALT, encode(salt))
                putInt(KEY_PIN_ITERATIONS, VaultCrypto.newKeyIterations)
                remove(KEY_PIN_HASH)
            }
            created
        } else {
            null
        }
        if (dek == null) return false
        install(dek)
        return true
    }

    override fun getRecoveryQuestionId(): Int? {
        if (!prefs.contains(KEY_RECOVERY_QUESTION_ID)) return null
        return prefs.getInt(KEY_RECOVERY_QUESTION_ID, -1).takeIf { it >= 0 }
    }

    override fun verifyRecoveryAnswer(answer: String): Boolean {
        val trimmed = answer.trim()
        val wrap = prefs.getString(KEY_RECOVERY_WRAP, null)
        val dek = if (wrap != null) {
            val salt = decode(prefs.getString(KEY_RECOVERY_SALT, null) ?: return false)
            VaultCrypto.unwrap(
                wrap,
                trimmed.toCharArray(),
                salt,
                prefs.getInt(KEY_RECOVERY_ITERATIONS, VaultCrypto.newKeyIterations),
            )
        } else if (legacyRecoveryMatches(trimmed)) {
            val created = VaultSession.takeIf { it.isOpen() }?.require() ?: VaultCrypto.randomDek()
            val salt = VaultCrypto.randomSalt()
            val normalized = trimmed.lowercase()
            prefs.edit {
                putString(KEY_RECOVERY_WRAP, VaultCrypto.wrap(created, normalized.toCharArray(), salt))
                putString(KEY_RECOVERY_SALT, encode(salt))
                putInt(KEY_RECOVERY_ITERATIONS, VaultCrypto.newKeyIterations)
                remove(KEY_RECOVERY_HASH)
            }
            created
        } else {
            null
        }
        if (dek == null) return false
        install(dek)
        return true
    }

    override fun resetPin(newPin: String): Boolean {
        if (!VaultSession.isOpen()) return false
        val dek = VaultSession.require()
        val salt = VaultCrypto.randomSalt()
        prefs.edit {
            putString(KEY_PIN_WRAP, VaultCrypto.wrap(dek, newPin.toCharArray(), salt))
            putString(KEY_PIN_SALT, encode(salt))
            putInt(KEY_PIN_ITERATIONS, VaultCrypto.newKeyIterations)
            remove(KEY_PIN_HASH)
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKED_UNTIL, 0L)
            putLong(KEY_PIN_CHANGED_AT, System.currentTimeMillis())
        }
        return true
    }

    override fun unlockMethod(): UnlockMethod {
        return when (prefs.getString(KEY_METHOD, null)) {
            "pin" -> UnlockMethod.PIN
            "device" -> UnlockMethod.DEVICE
            "none" -> UnlockMethod.NONE
            else -> if (isPinSetupComplete()) UnlockMethod.PIN else UnlockMethod.UNSET
        }
    }

    override fun setUnlockMethod(method: UnlockMethod) {
        prefs.edit { putString(KEY_METHOD, method.name.lowercase()) }
    }

    override fun unlockWithOpenKey(): Boolean {
        val payload = decode(prefs.getString(KEY_OPEN_WRAP, null) ?: return false)
        val dek = keystore?.openUnwrap(payload) ?: return false
        if (dek.size != 32) return false
        install(dek)
        return true
    }

    override fun enableNoAuthentication(): Boolean {
        val dek = if (VaultSession.isOpen()) VaultSession.require() else VaultCrypto.randomDek()
        val wrapped = keystore?.openWrap(dek) ?: return false
        prefs.edit {
            putString(KEY_OPEN_WRAP, encode(wrapped))
            putString(KEY_METHOD, "none")
            remove(KEY_PIN_WRAP)
            remove(KEY_PIN_HASH)
            remove(KEY_PIN_SALT)
            remove(KEY_RECOVERY_WRAP)
            remove(KEY_RECOVERY_HASH)
            remove(KEY_RECOVERY_SALT)
            remove(KEY_RECOVERY_QUESTION_ID)
        }
        if (!VaultSession.isOpen()) install(dek)
        return true
    }

    override fun prepareDeviceLock(): Boolean {
        if (!VaultSession.isOpen()) {
            install(VaultCrypto.randomDek())
        }
        prefs.edit { putString(KEY_METHOD, "device") }
        return biometricEnrollCipher() != null || hasBiometricWrap()
    }

    override fun wrapCurrentKeyWithPin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String): Boolean {
        if (!VaultSession.isOpen()) return false
        val dek = VaultSession.require()
        val pinSalt = VaultCrypto.randomSalt()
        val answerSalt = VaultCrypto.randomSalt()
        prefs.edit {
            putString(KEY_PIN_WRAP, VaultCrypto.wrap(dek, pin.toCharArray(), pinSalt))
            putString(KEY_PIN_SALT, encode(pinSalt))
            putInt(KEY_PIN_ITERATIONS, VaultCrypto.newKeyIterations)
            putInt(KEY_RECOVERY_QUESTION_ID, recoveryQuestionId)
            putString(KEY_RECOVERY_WRAP, VaultCrypto.wrap(dek, recoveryAnswer.trim().toCharArray(), answerSalt))
            putString(KEY_RECOVERY_SALT, encode(answerSalt))
            putInt(KEY_RECOVERY_ITERATIONS, VaultCrypto.newKeyIterations)
            remove(KEY_PIN_HASH)
            remove(KEY_RECOVERY_HASH)
            putString(KEY_METHOD, "pin")
            putLong(KEY_PIN_CHANGED_AT, System.currentTimeMillis())
        }
        return true
    }

    override fun failedPinAttempts(): Int {
        if (pinLockedUntil() > System.currentTimeMillis()) return MAX_ATTEMPTS
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    override fun pinLockedUntil(): Long = prefs.getLong(KEY_LOCKED_UNTIL, 0L)

    override fun registerFailedPin(): Int = registerFailure(KEY_FAILED_ATTEMPTS, KEY_LOCKED_UNTIL)

    override fun clearPinFailures() {
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKED_UNTIL, 0L)
        }
    }

    override fun failedRecoveryAttempts(): Int {
        if (recoveryLockedUntil() > System.currentTimeMillis()) return MAX_ATTEMPTS
        return prefs.getInt(KEY_RECOVERY_FAILED, 0)
    }

    override fun recoveryLockedUntil(): Long = prefs.getLong(KEY_RECOVERY_LOCKED_UNTIL, 0L)

    override fun registerFailedRecovery(): Int = registerFailure(KEY_RECOVERY_FAILED, KEY_RECOVERY_LOCKED_UNTIL)

    override fun clearRecoveryFailures() {
        prefs.edit {
            putInt(KEY_RECOVERY_FAILED, 0)
            putLong(KEY_RECOVERY_LOCKED_UNTIL, 0L)
        }
    }

    override fun hasBiometricWrap(): Boolean = prefs.contains(KEY_BIO_WRAP)

    override fun biometricUnlockCipher(): Cipher? {
        val payload = decode(prefs.getString(KEY_BIO_WRAP, null) ?: return null)
        if (payload.size <= IV_LENGTH) return null
        return keystore?.decryptCipher(payload.copyOfRange(0, IV_LENGTH))
    }

    override fun completeBiometricUnlock(cipher: Cipher): Boolean {
        val payload = decode(prefs.getString(KEY_BIO_WRAP, null) ?: return false)
        val dek = try {
            cipher.doFinal(payload, IV_LENGTH, payload.size - IV_LENGTH)
        } catch (_: Exception) {
            return false
        }
        if (dek.size != 32) return false
        install(dek)
        return true
    }

    override fun biometricEnrollCipher(): Cipher? = keystore?.encryptCipher()

    override fun completeBiometricEnroll(cipher: Cipher): Boolean {
        if (!VaultSession.isOpen()) return false
        val encrypted = try {
            cipher.doFinal(VaultSession.require())
        } catch (_: Exception) {
            return false
        }
        val iv = cipher.iv ?: return false
        prefs.edit { putString(KEY_BIO_WRAP, encode(iv + encrypted)) }
        return true
    }

    private fun install(dek: ByteArray) {
        VaultSession.install(dek)
        onKeyInstalled()
    }

    private fun legacyPinMatches(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val candidate = PinHasher.hash(pin, salt)
        return sha256(candidate).constantTimeEquals(sha256(stored))
    }

    private fun legacyRecoveryMatches(answer: String): Boolean {
        val salt = prefs.getString(KEY_RECOVERY_SALT, null) ?: return false
        val stored = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        val candidate = PinHasher.hash(answer.trim().lowercase(), salt)
        return sha256(candidate).constantTimeEquals(sha256(stored))
    }

    private fun registerFailure(countKey: String, untilKey: String): Int {
        val next = (prefs.getInt(countKey, 0) + 1).coerceAtMost(MAX_ATTEMPTS)
        prefs.edit {
            putInt(countKey, next)
            if (next >= MAX_ATTEMPTS) putLong(untilKey, System.currentTimeMillis() + LOCKOUT_MS)
        }
        return next
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    private companion object {
        const val IV_LENGTH = 12
        const val KEY_PIN_CHANGED_AT = "pin_changed_at"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_WRAP = "pin_wrap"
        const val KEY_PIN_ITERATIONS = "pin_iterations"
        const val KEY_RECOVERY_QUESTION_ID = "recovery_question_id"
        const val KEY_RECOVERY_SALT = "recovery_salt"
        const val KEY_RECOVERY_HASH = "recovery_hash"
        const val KEY_RECOVERY_WRAP = "recovery_wrap"
        const val KEY_RECOVERY_ITERATIONS = "recovery_iterations"
        const val KEY_BIO_WRAP = "bio_wrap"
        const val KEY_OPEN_WRAP = "open_wrap"
        const val KEY_METHOD = "unlock_method"
        const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        const val KEY_LOCKED_UNTIL = "pin_locked_until"
        const val KEY_RECOVERY_FAILED = "recovery_failed_attempts"
        const val KEY_RECOVERY_LOCKED_UNTIL = "recovery_locked_until"
        const val MAX_ATTEMPTS = 5
        const val LOCKOUT_MS = 10 * 60 * 1000L
    }
}
