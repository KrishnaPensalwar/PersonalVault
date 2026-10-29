package com.example.sample.security.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.sample.security.PinHasher

class AppLockRepositoryImpl(
    private val prefs: SharedPreferences,
) : AppLockRepository {

    override fun isPinSetupComplete(): Boolean =
        prefs.contains(KEY_PIN_HASH)

    override fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        val pinSalt = PinHasher.generateSalt()
        val answerSalt = PinHasher.generateSalt()
        prefs.edit {
            putString(KEY_PIN_SALT, pinSalt)
            putString(KEY_PIN_HASH, PinHasher.hash(pin, pinSalt))
            putInt(KEY_RECOVERY_QUESTION_ID, recoveryQuestionId)
            putString(KEY_RECOVERY_SALT, answerSalt)
            putString(KEY_RECOVERY_HASH, PinHasher.hash(normalizeAnswer(recoveryAnswer), answerSalt))
        }
    }

    override fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return PinHasher.hash(pin, salt) == storedHash
    }

    override fun getRecoveryQuestionId(): Int? {
        if (!prefs.contains(KEY_RECOVERY_QUESTION_ID)) return null
        return prefs.getInt(KEY_RECOVERY_QUESTION_ID, -1).takeIf { it >= 0 }
    }

    override fun verifyRecoveryAnswer(answer: String): Boolean {
        val salt = prefs.getString(KEY_RECOVERY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        return PinHasher.hash(normalizeAnswer(answer), salt) == storedHash
    }

    override fun resetPin(newPin: String) {
        val pinSalt = PinHasher.generateSalt()
        prefs.edit {
            putString(KEY_PIN_SALT, pinSalt)
            putString(KEY_PIN_HASH, PinHasher.hash(newPin, pinSalt))
        }
    }

    private fun normalizeAnswer(answer: String): String = answer.trim().lowercase()

    private companion object {
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_RECOVERY_QUESTION_ID = "recovery_question_id"
        const val KEY_RECOVERY_SALT = "recovery_salt"
        const val KEY_RECOVERY_HASH = "recovery_hash"
    }
}
