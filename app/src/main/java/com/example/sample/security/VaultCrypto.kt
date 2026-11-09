package com.example.sample.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM and PBKDF2-HMAC-SHA256. The data key never leaves memory except as
 * ciphertext produced by [wrap].
 */
object VaultCrypto {

    const val ITERATIONS = 120_000

    /** Production iteration count. Tests may lower this so the suite stays fast. */
    @Volatile
    var newKeyIterations: Int = ITERATIONS
    private const val GCM_TAG_BITS = 128
    private const val IV_LENGTH = 12
    private const val DEK_LENGTH = 32

    private val random = SecureRandom()

    fun randomDek(): ByteArray = ByteArray(DEK_LENGTH).also(random::nextBytes)

    fun randomSalt(): ByteArray = ByteArray(16).also(random::nextBytes)

    fun wrap(dek: ByteArray, passphrase: CharArray, salt: ByteArray, iterations: Int = newKeyIterations): String {
        val key = derive(passphrase, salt, iterations)
        return Base64.getEncoder().encodeToString(encrypt(key, dek))
    }

    fun unwrap(wrapped: String, passphrase: CharArray, salt: ByteArray, iterations: Int = newKeyIterations): ByteArray? {
        return try {
            val key = derive(passphrase, salt, iterations)
            val dek = decrypt(key, Base64.getDecoder().decode(wrapped))
            if (dek.size == DEK_LENGTH) dek else null
        } catch (_: Exception) {
            null
        }
    }

    fun encrypt(keyBytes: ByteArray, plaintext: ByteArray): ByteArray {
        val key = SecretKeySpec(keyBytes, "AES")
        return encrypt(key, plaintext)
    }

    fun decrypt(keyBytes: ByteArray, payload: ByteArray): ByteArray {
        val key = SecretKeySpec(keyBytes, "AES")
        return decrypt(key, payload)
    }

    fun sealString(keyBytes: ByteArray, plain: String): String {
        val payload = encrypt(keyBytes, plain.toByteArray(Charsets.UTF_8))
        return "enc1:" + Base64.getEncoder().encodeToString(payload)
    }

    fun openString(keyBytes: ByteArray, stored: String): String {
        if (!stored.startsWith("enc1:")) return stored
        val payload = Base64.getDecoder().decode(stored.removePrefix("enc1:"))
        return decrypt(keyBytes, payload).toString(Charsets.UTF_8)
    }

    fun isSealed(stored: String): Boolean = stored.startsWith("enc1:")

    private fun encrypt(key: SecretKeySpec, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(plaintext)
        return cipher.iv + ciphertext
    }

    private fun decrypt(key: SecretKeySpec, payload: ByteArray): ByteArray {
        require(payload.size > IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = payload.copyOfRange(0, IV_LENGTH)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(payload, IV_LENGTH, payload.size - IV_LENGTH)
    }

    private fun derive(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, "AES")
    }
}

fun ByteArray.constantTimeEquals(other: ByteArray): Boolean = MessageDigest.isEqual(this, other)
