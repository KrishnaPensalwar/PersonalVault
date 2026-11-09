package com.example.sample.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * SHA-256 + random salt hashing for the app PIN and recovery answer.
 * Never store [hash] results without their matching salt.
 */
object PinHasher {

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(value: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hashed = digest.digest(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashed)
    }
}
