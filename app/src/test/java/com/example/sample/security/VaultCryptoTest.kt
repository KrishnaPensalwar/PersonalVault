package com.example.sample.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class VaultCryptoTest {

    @Before
    fun setUp() {
        VaultCrypto.newKeyIterations = 1_000
        VaultSession.clear()
    }

    @Test
    fun pinWrapRoundTripAndWrongPinFails() {
        val dek = VaultCrypto.randomDek()
        val salt = VaultCrypto.randomSalt()
        val wrapped = VaultCrypto.wrap(dek, "123456".toCharArray(), salt)
        assertArrayEquals(dek, VaultCrypto.unwrap(wrapped, "123456".toCharArray(), salt))
        assertNull(VaultCrypto.unwrap(wrapped, "000000".toCharArray(), salt))
    }

    @Test
    fun recoveryPassphraseIsCaseSensitive() {
        val dek = VaultCrypto.randomDek()
        val salt = VaultCrypto.randomSalt()
        val wrapped = VaultCrypto.wrap(dek, "BlueHarbor12".toCharArray(), salt)
        assertArrayEquals(dek, VaultCrypto.unwrap(wrapped, "BlueHarbor12".toCharArray(), salt))
        assertNull(VaultCrypto.unwrap(wrapped, "blueharbor12".toCharArray(), salt))
    }

    @Test
    fun modifiedCiphertextDoesNotDecrypt() {
        val key = VaultCrypto.randomDek()
        val sealed = VaultCrypto.sealString(key, "secret")
        val tampered = sealed.dropLast(2) + "aa"
        try {
            VaultCrypto.openString(key, tampered)
            throw AssertionError("tampered ciphertext was accepted")
        } catch (_: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun documentMigrationEncryptsOnceAndSurvivesRetry() {
        val dir = File(System.getProperty("java.io.tmpdir"), "vault-test-${System.nanoTime()}")
        dir.mkdirs()
        val file = File(dir, "note")
        file.writeBytes("hello".toByteArray())
        VaultSession.install(VaultCrypto.randomDek())
        DocumentSealing.migrateFile(file)
        assertTrue(DocumentSealing.isEncrypted(file))
        assertEquals("hello", DocumentSealing.read(file).toString(Charsets.UTF_8))
        val encrypted = file.readBytes()
        DocumentSealing.migrateFile(file)
        assertArrayEquals(encrypted, file.readBytes())
        encrypted[encrypted.size - 1] = encrypted[encrypted.size - 1].inc()
        file.writeBytes(encrypted)
        try {
            DocumentSealing.read(file)
            throw AssertionError("modified document was accepted")
        } catch (_: Exception) {
            assertNotEquals("hello", "")
        } finally {
            dir.deleteRecursively()
            VaultSession.clear()
        }
    }

    @Test
    fun interruptedMigrationLeavesTheOriginalUntilRewriteFinishes() {
        val dir = File(System.getProperty("java.io.tmpdir"), "vault-test-${System.nanoTime()}")
        dir.mkdirs()
        val file = File(dir, "note")
        file.writeBytes("plain".toByteArray())
        File(dir, "note.migrating").writeBytes("partial".toByteArray())
        VaultSession.install(VaultCrypto.randomDek())
        DocumentSealing.migrateFile(file)
        assertEquals("plain", DocumentSealing.read(file).toString(Charsets.UTF_8))
        assertFalse(File(dir, "note.migrating").exists())
        dir.deleteRecursively()
    }

    @Test
    fun sessionClearRemovesTheDataKey() {
        VaultSession.install(VaultCrypto.randomDek())
        assertTrue(VaultSession.isOpen())
        VaultSession.clear()
        assertFalse(VaultSession.isOpen())
    }
}
