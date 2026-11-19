package com.example.sample.security

import com.example.sample.security.data.AppLockRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.example.sample.security.data.AppLockRepositoryImpl
import com.example.sample.security.data.FakeSecureStorageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test

class ChangePinViewModelTest {

    @Before
    fun setUp() {
        VaultCrypto.newKeyIterations = 1_000
        VaultSession.clear()
    }

    @Test
    fun `correct current pin rewraps and rejects the old pin`() {
        val repository = MemoryLock()
        repository.savePin("123456", 0, "long-passphrase")
        val viewModel = ChangePinViewModel(repository)

        viewModel.onSubmit("123456", "654321", "654321")
        viewModel.awaitResult()

        assertTrue(viewModel.state.value.saved)
        assertFalse(repository.verifyPin("123456"))
        assertTrue(repository.verifyPin("654321"))
        assertTrue(repository.pinChangedAt() > 0L)
    }

    @Test
    fun `wrong current pin does not change the wrap`() {
        val repository = MemoryLock()
        repository.savePin("123456", 0, "long-passphrase")
        val viewModel = ChangePinViewModel(repository)

        viewModel.onSubmit("000000", "654321", "654321")
        viewModel.awaitResult()

        assertEquals("Incorrect PIN", viewModel.state.value.error)
        assertTrue(repository.verifyPin("123456"))
        assertEquals(1, repository.failedPinAttempts())
    }
}

private fun ChangePinViewModel.awaitResult() = runBlocking {
    withTimeout(15_000) {
        while (state.value.saving) delay(20)
    }
}

private class MemoryLock : AppLockRepository by AppLockRepositoryImpl(FakeSecureStorageManager())
