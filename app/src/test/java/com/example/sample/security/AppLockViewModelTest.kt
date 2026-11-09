package com.example.sample.security

import com.example.sample.security.data.AppLockRepository
import com.example.sample.security.data.UnlockMethod
import javax.crypto.Cipher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAppLockRepository : AppLockRepository {
    var setupComplete = false
    var pin: String? = null
    var storedRecoveryQuestionId: Int? = null
    var recoveryAnswer: String? = null

    override fun isPinSetupComplete() = setupComplete
    override fun pinChangedAt() = 0L
    override fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        this.pin = pin
        this.storedRecoveryQuestionId = recoveryQuestionId
        this.recoveryAnswer = recoveryAnswer
        setupComplete = true
    }
    override fun verifyPin(pin: String) = this.pin == pin
    override fun getRecoveryQuestionId() = storedRecoveryQuestionId
    override fun verifyRecoveryAnswer(answer: String) =
        recoveryAnswer?.trim()?.lowercase() == answer.trim().lowercase()
    override fun resetPin(newPin: String): Boolean {
        pin = newPin
        return true
    }
    var failures = 0
    var lockedUntil = 0L
    override fun failedPinAttempts() = if (lockedUntil > System.currentTimeMillis()) 5 else failures
    override fun pinLockedUntil() = lockedUntil
    override fun registerFailedPin(): Int {
        failures = (failures + 1).coerceAtMost(5)
        if (failures >= 5) lockedUntil = System.currentTimeMillis() + 10 * 60_000L
        return failures
    }
    override fun clearPinFailures() {
        failures = 0
        lockedUntil = 0L
    }
    var recoveryFailures = 0
    var recoveryLockedUntil = 0L
    override fun failedRecoveryAttempts() = if (recoveryLockedUntil > System.currentTimeMillis()) 5 else recoveryFailures
    override fun recoveryLockedUntil() = recoveryLockedUntil
    override fun registerFailedRecovery(): Int {
        recoveryFailures = (recoveryFailures + 1).coerceAtMost(5)
        if (recoveryFailures >= 5) recoveryLockedUntil = System.currentTimeMillis() + 10 * 60_000L
        return recoveryFailures
    }
    override fun clearRecoveryFailures() {
        recoveryFailures = 0
        recoveryLockedUntil = 0L
    }
    override fun hasBiometricWrap() = false
    override fun biometricUnlockCipher(): Cipher? = null
    override fun completeBiometricUnlock(cipher: Cipher) = false
    override fun biometricEnrollCipher(): Cipher? = null
    override fun completeBiometricEnroll(cipher: Cipher) = false
    var method = UnlockMethod.UNSET
    override fun unlockMethod() = if (method == UnlockMethod.UNSET && setupComplete) UnlockMethod.PIN else method
    override fun setUnlockMethod(method: UnlockMethod) { this.method = method }
    override fun unlockWithOpenKey() = false
    override fun enableNoAuthentication() = false
    override fun prepareDeviceLock() = false
    override fun wrapCurrentKeyWithPin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String) = false
}

private class FakeBiometricAvailabilityChecker(var available: Boolean) : BiometricAvailabilityChecker {
    override fun isAvailable() = available
}

class AppLockViewModelTest {

    @Test
    fun `biometric hardware without a wrapped key shows PIN setup`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(true))
        assertTrue(viewModel.lockState.value is LockScreenState.ChooseUnlock)
    }

    @Test
    fun `fresh install asks for an unlock method`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(false))
        assertTrue(viewModel.lockState.value is LockScreenState.ChooseUnlock)
    }

    @Test
    fun `choosing a PIN opens PIN setup`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(false))
        viewModel.onChoosePin()
        assertTrue(viewModel.lockState.value is LockScreenState.PinSetup)
    }

    @Test
    fun `no biometric and PIN already set shows PinEntry`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 0, "Rex") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        assertEquals(LockScreenState.PinEntry(), viewModel.lockState.value)
    }

    @Test
    fun `successful pin setup unlocks and saves to repository`() = runTest {
        val repository = FakeAppLockRepository()
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))

        viewModel.onPinSetupSubmit("123456", "123456", 1, "SmithFamily12")

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
        assertTrue(repository.isPinSetupComplete())
    }

    @Test
    fun `mismatched confirm pin keeps PinSetup with error`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(false))

        viewModel.onPinSetupSubmit("123456", "654321", 1, "Smith")

        assertTrue(viewModel.lockState.value is LockScreenState.PinSetup)
        assertEquals("PINs do not match", (viewModel.lockState.value as LockScreenState.PinSetup).error)
    }

    @Test
    fun `correct pin entry unlocks`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 0, "Rex") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))

        viewModel.onPinEntrySubmit("123456")

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
    }

    @Test
    fun `wrong pin entry shows error and stays locked`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 0, "Rex") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))

        viewModel.onPinEntrySubmit("000000")

        val state = viewModel.lockState.value as LockScreenState.PinEntry
        assertEquals("Incorrect PIN", state.error)
        assertEquals(1, state.failedAttempts)
    }

    @Test
    fun `forgot pin shows the saved recovery question`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onPinEntrySubmit("000000") // land on PinEntry first

        viewModel.onForgotPinClicked()

        val state = viewModel.lockState.value
        assertTrue(state is LockScreenState.RecoveryPrompt)
        assertEquals(RecoveryQuestion.FIRST_SCHOOL, (state as LockScreenState.RecoveryPrompt).question)
    }

    @Test
    fun `correct recovery answer moves to SetNewPin`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()

        viewModel.onRecoveryAnswerSubmit("Lincoln High")

        assertEquals(LockScreenState.SetNewPin(), viewModel.lockState.value)
    }

    @Test
    fun `wrong recovery answer shows error`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()

        viewModel.onRecoveryAnswerSubmit("wrong")

        assertEquals(LockScreenState.RecoveryPrompt(RecoveryQuestion.FIRST_SCHOOL, "Incorrect answer"), viewModel.lockState.value)
    }

    @Test
    fun `setting a new pin after recovery unlocks and updates repository`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()
        viewModel.onRecoveryAnswerSubmit("Lincoln High")

        viewModel.onSetNewPinSubmit("567890", "567890")

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
        assertTrue(repository.verifyPin("567890"))
    }

    @Test
    fun `biometric callback without a cipher does not unlock`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(true))

        viewModel.onBiometricUnlock(null)

        assertTrue(viewModel.lockState.value !is LockScreenState.Unlocked)
    }

    fun `five wrong recovery answers lock recovery for ten minutes`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("123456", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()
        repeat(5) { viewModel.onRecoveryAnswerSubmit("wrong-answer") }

        val state = viewModel.lockState.value as LockScreenState.RecoveryPrompt
        assertEquals("Too many attempts. Locked for 10 minutes", state.error)
        assertTrue(repository.recoveryLockedUntil() > System.currentTimeMillis())
    }
}
