package com.example.sample.security

import com.example.sample.security.data.AppLockRepository
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
    override fun resetPin(newPin: String) { pin = newPin }
}

private class FakeBiometricAvailabilityChecker(var available: Boolean) : BiometricAvailabilityChecker {
    override fun isAvailable() = available
}

class AppLockViewModelTest {

    @Test
    fun `biometric available shows Biometric state`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(true))
        assertEquals(LockScreenState.Biometric, viewModel.lockState.value)
    }

    @Test
    fun `no biometric and no PIN set shows PinSetup`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(false))
        assertEquals(LockScreenState.PinSetup(), viewModel.lockState.value)
    }

    @Test
    fun `no biometric and PIN already set shows PinEntry`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 0, "Rex") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        assertEquals(LockScreenState.PinEntry(), viewModel.lockState.value)
    }

    @Test
    fun `successful pin setup unlocks and saves to repository`() = runTest {
        val repository = FakeAppLockRepository()
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))

        viewModel.onPinSetupSubmit("1234", "1234", 1, "Smith")

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
        assertTrue(repository.isPinSetupComplete())
    }

    @Test
    fun `mismatched confirm pin keeps PinSetup with error`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(false))

        viewModel.onPinSetupSubmit("1234", "4321", 1, "Smith")

        assertTrue(viewModel.lockState.value is LockScreenState.PinSetup)
        assertEquals("PINs do not match", (viewModel.lockState.value as LockScreenState.PinSetup).error)
    }

    @Test
    fun `correct pin entry unlocks`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 0, "Rex") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))

        viewModel.onPinEntrySubmit("1234")

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
    }

    @Test
    fun `wrong pin entry shows error and stays locked`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 0, "Rex") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))

        viewModel.onPinEntrySubmit("0000")

        assertEquals(LockScreenState.PinEntry("Incorrect PIN"), viewModel.lockState.value)
    }

    @Test
    fun `forgot pin shows the saved recovery question`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onPinEntrySubmit("0000") // land on PinEntry first

        viewModel.onForgotPinClicked()

        val state = viewModel.lockState.value
        assertTrue(state is LockScreenState.RecoveryPrompt)
        assertEquals(RecoveryQuestion.FIRST_SCHOOL, (state as LockScreenState.RecoveryPrompt).question)
    }

    @Test
    fun `correct recovery answer moves to SetNewPin`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()

        viewModel.onRecoveryAnswerSubmit("Lincoln High")

        assertEquals(LockScreenState.SetNewPin(), viewModel.lockState.value)
    }

    @Test
    fun `wrong recovery answer shows error`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()

        viewModel.onRecoveryAnswerSubmit("wrong")

        assertEquals(LockScreenState.RecoveryPrompt(RecoveryQuestion.FIRST_SCHOOL, "Incorrect answer"), viewModel.lockState.value)
    }

    @Test
    fun `setting a new pin after recovery unlocks and updates repository`() = runTest {
        val repository = FakeAppLockRepository().apply { savePin("1234", 2, "Lincoln High") }
        val viewModel = AppLockViewModel(repository, FakeBiometricAvailabilityChecker(false))
        viewModel.onForgotPinClicked()
        viewModel.onRecoveryAnswerSubmit("Lincoln High")

        viewModel.onSetNewPinSubmit("5678", "5678")

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
        assertTrue(repository.verifyPin("5678"))
    }

    @Test
    fun `onBiometricSuccess unlocks`() = runTest {
        val viewModel = AppLockViewModel(FakeAppLockRepository(), FakeBiometricAvailabilityChecker(true))

        viewModel.onBiometricSuccess()

        assertEquals(LockScreenState.Unlocked, viewModel.lockState.value)
    }
}
