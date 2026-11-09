package com.example.sample.security.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.example.sample.security.VaultCrypto
import com.example.sample.security.VaultSession
import org.junit.Before
import org.junit.Test

class AppLockRepositoryImplTest {

    private lateinit var repository: AppLockRepository

    @Before
    fun setUp() {
        VaultCrypto.newKeyIterations = 1_000
        VaultSession.clear()
        repository = AppLockRepositoryImpl(FakeSharedPreferences())
    }

    @Test
    fun `setup is not complete before a PIN is saved`() {
        assertFalse(repository.isPinSetupComplete())
    }

    @Test
    fun `setup is complete and PIN verifies after savePin`() {
        repository.savePin(pin = "1234", recoveryQuestionId = 0, recoveryAnswer = "Rex")

        assertTrue(repository.isPinSetupComplete())
        assertTrue(repository.verifyPin("1234"))
        assertFalse(repository.verifyPin("0000"))
    }

    @Test
    fun `recovery question id and answer verification round-trip`() {
        repository.savePin(pin = "1234", recoveryQuestionId = 2, recoveryAnswer = "Lincoln High")

        assertEquals(2, repository.getRecoveryQuestionId())
        assertTrue(repository.verifyRecoveryAnswer("Lincoln High"))
        assertTrue(repository.verifyRecoveryAnswer("  Lincoln High  "))
        assertFalse(repository.verifyRecoveryAnswer("lincoln high"))
        assertFalse(repository.verifyRecoveryAnswer("wrong"))
    }

    @Test
    fun `getRecoveryQuestionId is null before setup`() {
        assertNull(repository.getRecoveryQuestionId())
    }

    @Test
    fun `resetPin changes which PIN verifies`() {
        repository.savePin(pin = "1234", recoveryQuestionId = 0, recoveryAnswer = "Rex")

        repository.resetPin("5678")

        assertFalse(repository.verifyPin("1234"))
        assertTrue(repository.verifyPin("5678"))
    }

    @Test
    fun `five wrong recovery answers lock for ten minutes`() {
        repository.savePin(pin = "1234", recoveryQuestionId = 0, recoveryAnswer = "long-passphrase")
        repeat(5) {
            assertFalse(repository.verifyRecoveryAnswer("nope"))
            repository.registerFailedRecovery()
        }
        assertEquals(5, repository.failedRecoveryAttempts())
        assertTrue(repository.recoveryLockedUntil() > System.currentTimeMillis())
    }

    @Test
    fun `resetPin keeps the same data key`() {
        repository.savePin(pin = "1234", recoveryQuestionId = 0, recoveryAnswer = "long-passphrase")
        assertTrue(repository.resetPin("5678"))
        VaultSession.clear()
        assertTrue(repository.verifyPin("5678"))
        assertTrue(repository.verifyRecoveryAnswer("long-passphrase"))
    }
}
