# App Lock (PIN / Biometric / Device Credential) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Gate the whole app behind an unlock screen on cold start and after being backgrounded past a configurable timeout, preferring the device's existing biometric/PIN/pattern/password and falling back to an app-managed 4-digit PIN + recovery question when the device has nothing enrolled.

**Architecture:** A `security` package holds a Koin-provided `AppLockRepository` (EncryptedSharedPreferences-backed) for PIN/recovery storage, an `AppLockViewModel` that decides between biometric/device-credential (`androidx.biometric.BiometricPrompt`) and the app PIN flow, and a Compose `AppLockGate` that wraps `AppNavigation` in `MainActivity` and shows `LockScreen` until unlocked. A process-wide `AppBackgroundTracker` (`ProcessLifecycleOwner` observer) decides whether re-entering the foreground should re-lock.

**Tech Stack:** Kotlin, Compose, Koin DI, `androidx.biometric:biometric`, `androidx.security:security-crypto`, `androidx.lifecycle:lifecycle-process`, `androidx.fragment:fragment-ktx` (for `FragmentActivity`, required by `BiometricPrompt`).

## Global Constraints

- PIN is always exactly 4 digits.
- Background lock timeout is `AppLockConfig.backgroundLockTimeout`, a mutable `Duration` defaulting to `5.minutes` — never hardcode the timeout elsewhere.
- No attempt-limiting/lockout on wrong PIN or wrong recovery answer (out of scope per spec).
- PIN and recovery answer are hashed (SHA-256 + random salt) before storage; never stored in plaintext.
- Recovery questions come from a fixed preset list of 5; no free-text custom questions.
- New library versions: `androidx.security:security-crypto:1.1.0`, `androidx.biometric:biometric:1.1.0`, `androidx.lifecycle:lifecycle-process:2.11.0` and `androidx.lifecycle:lifecycle-runtime-compose:2.11.0` (both match existing `lifecycleRuntimeKtx`), `androidx.fragment:fragment-ktx:1.9.1`.
- Follow existing project conventions: `StateFlow` for screen state, `MutableSharedFlow`/`asSharedFlow()` for one-shot events, sealed interfaces for UI state, repository interface + impl pair, Koin `single`/`viewModel` registration in `appModule`.

---

### Task 1: Add new dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: version catalog aliases `androidx-security-crypto`, `androidx-biometric`, `androidx-lifecycle-process`, `androidx-lifecycle-runtime-compose`, `androidx-fragment-ktx`, consumed by every later task that needs these libraries.

- [ ] **Step 1: Add version + library entries to the catalog**

In `gradle/libs.versions.toml`, add to `[versions]` (after `koin = "4.2.2"`):

```toml
securityCrypto = "1.1.0"
biometric = "1.1.0"
fragmentKtx = "1.9.1"
```

Add to `[libraries]` (anywhere after the existing `koin-androidx-compose` line):

```toml
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
androidx-lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-fragment-ktx = { group = "androidx.fragment", name = "fragment-ktx", version.ref = "fragmentKtx" }
```

- [ ] **Step 2: Add the dependencies to the app module**

In `app/build.gradle.kts`, append inside the `dependencies { ... }` block (after `implementation(libs.koin.androidx.compose)`):

```kotlin
implementation(libs.androidx.security.crypto)
implementation(libs.androidx.biometric)
implementation(libs.androidx.lifecycle.process)
implementation(libs.androidx.lifecycle.runtime.compose)
implementation(libs.androidx.fragment.ktx)
```

- [ ] **Step 3: Verify the project syncs**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath -q | grep -E "security-crypto|biometric|fragment-ktx|lifecycle-process|lifecycle-runtime-compose"`
Expected: five lines showing the resolved versions `1.1.0`, `1.1.0`, `1.9.1`, `2.11.0`, `2.11.0` with no resolution errors.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add biometric, security-crypto, fragment-ktx, lifecycle-process deps"
```

---

### Task 2: `AppLockConfig` and `PinHasher`

**Files:**
- Create: `app/src/main/java/com/example/sample/security/AppLockConfig.kt`
- Create: `app/src/main/java/com/example/sample/security/PinHasher.kt`
- Test: `app/src/test/java/com/example/sample/security/PinHasherTest.kt`

**Interfaces:**
- Produces: `AppLockConfig.backgroundLockTimeout: Duration` (var), `PinHasher.generateSalt(): String`, `PinHasher.hash(value: String, salt: String): String`. Both consumed by Task 3's repository and Task 6's background tracker.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.sample.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PinHasherTest {

    @Test
    fun `same value and salt produce the same hash`() {
        val salt = PinHasher.generateSalt()
        assertEquals(PinHasher.hash("1234", salt), PinHasher.hash("1234", salt))
    }

    @Test
    fun `different salts produce different hashes for the same value`() {
        val saltA = PinHasher.generateSalt()
        val saltB = PinHasher.generateSalt()
        assertNotEquals(PinHasher.hash("1234", saltA), PinHasher.hash("1234", saltB))
    }

    @Test
    fun `different values produce different hashes for the same salt`() {
        val salt = PinHasher.generateSalt()
        assertNotEquals(PinHasher.hash("1234", salt), PinHasher.hash("4321", salt))
    }

    @Test
    fun `generateSalt returns non-blank, non-equal values across calls`() {
        val saltA = PinHasher.generateSalt()
        val saltB = PinHasher.generateSalt()
        assertNotEquals(saltA, saltB)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.sample.security.PinHasherTest"`
Expected: FAIL to compile — `PinHasher` and `AppLockConfig` do not exist yet.

- [ ] **Step 3: Write `AppLockConfig`**

```kotlin
package com.example.sample.security

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * App-wide app-lock settings. [backgroundLockTimeout] is a mutable `var` (not
 * a constant) so it can be changed from one place at runtime — e.g. wired to
 * a future settings screen — without touching any call site, which always
 * reads [backgroundLockTimeout] fresh.
 */
object AppLockConfig {
    var backgroundLockTimeout: Duration = 5.minutes
}
```

- [ ] **Step 4: Write `PinHasher`**

```kotlin
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
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.sample.security.PinHasherTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/sample/security/AppLockConfig.kt app/src/main/java/com/example/sample/security/PinHasher.kt app/src/test/java/com/example/sample/security/PinHasherTest.kt
git commit -m "feat: add AppLockConfig and PinHasher"
```

---

### Task 3: `RecoveryQuestion` preset list

**Files:**
- Create: `app/src/main/java/com/example/sample/security/RecoveryQuestion.kt`

**Interfaces:**
- Produces: `enum class RecoveryQuestion(val id: Int, val text: String)` with `RecoveryQuestion.fromId(id: Int): RecoveryQuestion?`. Consumed by `AppLockRepository` (Task 4) and the recovery-question UI (Task 9).

- [ ] **Step 1: Write the enum**

```kotlin
package com.example.sample.security

/**
 * Fixed preset list of recovery questions used to reset a forgotten app PIN.
 * No free-text custom questions — keeps the recovery flow simple and avoids
 * users picking an easily-guessable question.
 */
enum class RecoveryQuestion(val id: Int, val text: String) {
    FIRST_PET(0, "What was the name of your first pet?"),
    MOTHERS_MAIDEN_NAME(1, "What is your mother's maiden name?"),
    FIRST_SCHOOL(2, "What was the name of your first school?"),
    FAVORITE_BOOK(3, "What is your favorite book?"),
    BIRTH_CITY(4, "What city were you born in?");

    companion object {
        fun fromId(id: Int): RecoveryQuestion? = entries.find { it.id == id }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/sample/security/RecoveryQuestion.kt
git commit -m "feat: add preset RecoveryQuestion list"
```

---

### Task 4: `AppLockRepository`

**Files:**
- Create: `app/src/main/java/com/example/sample/security/data/AppLockRepository.kt`
- Create: `app/src/main/java/com/example/sample/security/data/AppLockRepositoryImpl.kt`
- Test: `app/src/test/java/com/example/sample/security/data/AppLockRepositoryImplTest.kt`
- Test support: `app/src/test/java/com/example/sample/security/data/FakeSharedPreferences.kt`

**Interfaces:**
- Consumes: `PinHasher.generateSalt()`, `PinHasher.hash(value, salt)` from Task 2.
- Produces: `interface AppLockRepository` with `isPinSetupComplete(): Boolean`, `savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String)`, `verifyPin(pin: String): Boolean`, `getRecoveryQuestionId(): Int?`, `verifyRecoveryAnswer(answer: String): Boolean`, `resetPin(newPin: String)`. Consumed by `AppLockViewModel` (Task 7).

- [ ] **Step 1: Write a minimal fake `SharedPreferences` for tests**

`android.content.SharedPreferences` is just an interface (backed by the Android SDK stub jar at compile time), so it can be implemented directly in a JVM unit test without Robolectric.

```kotlin
package com.example.sample.security.data

import android.content.SharedPreferences

/** In-memory [SharedPreferences] fake for JVM unit tests. */
class FakeSharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?) =
        @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun contains(key: String?) = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val toRemove = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?) = apply { pending[key!!] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { pending[key!!] = values }
        override fun putInt(key: String?, value: Int) = apply { pending[key!!] = value }
        override fun putLong(key: String?, value: Long) = apply { pending[key!!] = value }
        override fun putFloat(key: String?, value: Float) = apply { pending[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { pending[key!!] = value }
        override fun remove(key: String?) = apply { toRemove.add(key!!) }
        override fun clear() = apply { clearAll = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) values.clear()
            toRemove.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}
```

- [ ] **Step 2: Write the failing repository test**

```kotlin
package com.example.sample.security.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockRepositoryImplTest {

    private lateinit var repository: AppLockRepository

    @Before
    fun setUp() {
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
        assertTrue(repository.verifyRecoveryAnswer("  lincoln high  ")) // trimmed, case-insensitive
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
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.sample.security.data.AppLockRepositoryImplTest"`
Expected: FAIL to compile — `AppLockRepository`/`AppLockRepositoryImpl` do not exist yet.

- [ ] **Step 4: Write the repository interface**

```kotlin
package com.example.sample.security.data

/**
 * Storage for the app-managed 4-digit PIN and its recovery question/answer.
 * Only used when the device itself has no biometric or device credential
 * enrolled — see [com.example.sample.security.BiometricAvailabilityChecker].
 */
interface AppLockRepository {

    fun isPinSetupComplete(): Boolean

    fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String)

    fun verifyPin(pin: String): Boolean

    /** Null until [savePin] has been called once. */
    fun getRecoveryQuestionId(): Int?

    fun verifyRecoveryAnswer(answer: String): Boolean

    fun resetPin(newPin: String)
}
```

- [ ] **Step 5: Write the repository implementation**

```kotlin
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
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.sample.security.data.AppLockRepositoryImplTest"`
Expected: PASS (5 tests).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/sample/security/data app/src/test/java/com/example/sample/security/data
git commit -m "feat: add AppLockRepository with hashed PIN and recovery storage"
```

---

### Task 5: `BiometricAvailabilityChecker`

**Files:**
- Create: `app/src/main/java/com/example/sample/security/BiometricAvailabilityChecker.kt`

**Interfaces:**
- Produces: `interface BiometricAvailabilityChecker { fun isAvailable(): Boolean }` and `class BiometricAvailabilityCheckerImpl(context: Context) : BiometricAvailabilityChecker`. Consumed by `AppLockViewModel` (Task 7); the interface (not the impl) lets Task 7's tests use a fake.

- [ ] **Step 1: Write the interface + impl**

No unit test here — it is a one-line wrapper around a final Android framework class (`BiometricManager`) that cannot be instantiated in a JVM unit test. `AppLockViewModel`'s tests (Task 7) cover the behavior by injecting a fake of this interface instead.

```kotlin
package com.example.sample.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Reports whether the device already has biometric enrollment OR a device
 * credential (PIN/pattern/password) set up — in either case we hand off to
 * [androidx.biometric.BiometricPrompt] instead of the app's own PIN flow.
 */
interface BiometricAvailabilityChecker {
    fun isAvailable(): Boolean
}

class BiometricAvailabilityCheckerImpl(
    private val context: Context,
) : BiometricAvailabilityChecker {

    override fun isAvailable(): Boolean {
        val result = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/sample/security/BiometricAvailabilityChecker.kt
git commit -m "feat: add BiometricAvailabilityChecker"
```

---

### Task 6: `AppBackgroundTracker`

**Files:**
- Create: `app/src/main/java/com/example/sample/security/AppBackgroundTracker.kt`
- Modify: `app/src/main/java/com/example/sample/MyApplication.kt`

**Interfaces:**
- Consumes: `AppLockConfig.backgroundLockTimeout` from Task 2.
- Produces: `object AppBackgroundTracker` with `fun consumeShouldLock(): Boolean`. Consumed by `AppLockViewModel`/`AppLockGate` (Task 7 & 8).

- [ ] **Step 1: Write `AppBackgroundTracker`**

```kotlin
package com.example.sample.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Process-wide tracker, registered on [androidx.lifecycle.ProcessLifecycleOwner]
 * from [com.example.sample.MyApplication]. Decides whether returning to the
 * foreground should re-trigger the lock screen: true on the very first
 * foregrounding (cold start) and whenever the app was backgrounded for at
 * least [AppLockConfig.backgroundLockTimeout].
 */
object AppBackgroundTracker : DefaultLifecycleObserver {

    private var backgroundedAtMillis: Long? = null
    private var shouldLockOnNextForeground: Boolean = true

    override fun onStop(owner: LifecycleOwner) {
        backgroundedAtMillis = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        val backgroundedAt = backgroundedAtMillis
        if (backgroundedAt != null) {
            val elapsedMillis = System.currentTimeMillis() - backgroundedAt
            shouldLockOnNextForeground = elapsedMillis >= AppLockConfig.backgroundLockTimeout.inWholeMilliseconds
        }
        // else: first-ever onStart (cold start) — shouldLockOnNextForeground stays true.
    }

    /** Reads and clears the pending lock decision. Call once per foregrounding. */
    fun consumeShouldLock(): Boolean {
        val result = shouldLockOnNextForeground
        shouldLockOnNextForeground = false
        return result
    }
}
```

- [ ] **Step 2: Register it in `MyApplication`**

```kotlin
package com.example.sample

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.sample.di.appModule
import com.example.sample.security.AppBackgroundTracker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppBackgroundTracker)
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/sample/security/AppBackgroundTracker.kt app/src/main/java/com/example/sample/MyApplication.kt
git commit -m "feat: add AppBackgroundTracker for background-timeout re-locking"
```

---

### Task 7: `AppLockViewModel` and lock state

**Files:**
- Create: `app/src/main/java/com/example/sample/security/LockScreenState.kt`
- Create: `app/src/main/java/com/example/sample/security/AppLockViewModel.kt`
- Test: `app/src/test/java/com/example/sample/security/AppLockViewModelTest.kt`

**Interfaces:**
- Consumes: `AppLockRepository` (Task 4), `BiometricAvailabilityChecker` (Task 5), `AppBackgroundTracker.consumeShouldLock()` (Task 6), `RecoveryQuestion` (Task 3).
- Produces: `AppLockViewModel(repository: AppLockRepository, biometricChecker: BiometricAvailabilityChecker)` with `lockState: StateFlow<LockScreenState>`, `events: SharedFlow<LockScreenEvent>`, and functions `onAppForegrounded()`, `onBiometricSuccess()`, `onBiometricFailed(message: String)`, `onPinSetupSubmit(pin, confirmPin, recoveryQuestionId, recoveryAnswer)`, `onPinEntrySubmit(pin)`, `onForgotPinClicked()`, `onRecoveryAnswerSubmit(answer)`, `onSetNewPinSubmit(pin, confirmPin)`, `onBackToPinEntry()`. Consumed by `LockScreen` (Task 9) and `AppModule` (Task 10).

- [ ] **Step 1: Write `LockScreenState`**

```kotlin
package com.example.sample.security

sealed interface LockScreenState {
    data object Checking : LockScreenState
    data object Biometric : LockScreenState
    data class PinEntry(val error: String? = null) : LockScreenState
    data class PinSetup(val error: String? = null) : LockScreenState
    /** Named `RecoveryPrompt`, not `RecoveryQuestion`, to avoid colliding with the top-level [com.example.sample.security.RecoveryQuestion] enum it wraps. */
    data class RecoveryPrompt(val question: RecoveryQuestion, val error: String? = null) : LockScreenState
    data class SetNewPin(val error: String? = null) : LockScreenState
    data object Unlocked : LockScreenState
}

sealed interface LockScreenEvent {
    data class Message(val text: String) : LockScreenEvent
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.example.sample.security

import com.example.sample.security.data.AppLockRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAppLockRepository : AppLockRepository {
    var setupComplete = false
    var pin: String? = null
    var recoveryQuestionId: Int? = null
    var recoveryAnswer: String? = null

    override fun isPinSetupComplete() = setupComplete
    override fun savePin(pin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        this.pin = pin
        this.recoveryQuestionId = recoveryQuestionId
        this.recoveryAnswer = recoveryAnswer
        setupComplete = true
    }
    override fun verifyPin(pin: String) = this.pin == pin
    override fun getRecoveryQuestionId() = recoveryQuestionId
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
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.sample.security.AppLockViewModelTest"`
Expected: FAIL to compile — `AppLockViewModel` does not exist yet.

- [ ] **Step 4: Write `AppLockViewModel`**

```kotlin
package com.example.sample.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.security.data.AppLockRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppLockViewModel(
    private val repository: AppLockRepository,
    private val biometricChecker: BiometricAvailabilityChecker,
) : ViewModel() {

    private val _lockState = MutableStateFlow<LockScreenState>(LockScreenState.Checking)
    val lockState: StateFlow<LockScreenState> = _lockState.asStateFlow()

    private val _events = MutableSharedFlow<LockScreenEvent>()
    val events = _events.asSharedFlow()

    init {
        evaluateLockState()
    }

    /** Called when the hosting lifecycle re-enters the foreground. */
    fun onAppForegrounded() {
        if (AppBackgroundTracker.consumeShouldLock()) {
            evaluateLockState()
        }
    }

    fun onBiometricSuccess() = unlock()

    fun onBiometricFailed(message: String) {
        viewModelScope.launch { _events.emit(LockScreenEvent.Message(message)) }
    }

    fun onPinSetupSubmit(pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) {
        val error = when {
            pin.length != 4 || pin.any { !it.isDigit() } -> "PIN must be 4 digits"
            pin != confirmPin -> "PINs do not match"
            recoveryAnswer.isBlank() -> "Answer is required"
            else -> null
        }
        if (error != null) {
            _lockState.update { LockScreenState.PinSetup(error) }
            return
        }
        repository.savePin(pin, recoveryQuestionId, recoveryAnswer)
        unlock()
    }

    fun onPinEntrySubmit(pin: String) {
        if (repository.verifyPin(pin)) {
            unlock()
        } else {
            _lockState.update { LockScreenState.PinEntry("Incorrect PIN") }
        }
    }

    fun onForgotPinClicked() {
        val question = repository.getRecoveryQuestionId()?.let(RecoveryQuestion::fromId) ?: return
        _lockState.update { LockScreenState.RecoveryPrompt(question) }
    }

    fun onRecoveryAnswerSubmit(answer: String) {
        val current = _lockState.value
        if (current !is LockScreenState.RecoveryPrompt) return
        if (repository.verifyRecoveryAnswer(answer)) {
            _lockState.update { LockScreenState.SetNewPin() }
        } else {
            _lockState.update { current.copy(error = "Incorrect answer") }
        }
    }

    fun onSetNewPinSubmit(pin: String, confirmPin: String) {
        val error = when {
            pin.length != 4 || pin.any { !it.isDigit() } -> "PIN must be 4 digits"
            pin != confirmPin -> "PINs do not match"
            else -> null
        }
        if (error != null) {
            _lockState.update { LockScreenState.SetNewPin(error) }
            return
        }
        repository.resetPin(pin)
        unlock()
    }

    fun onBackToPinEntry() {
        _lockState.update { LockScreenState.PinEntry() }
    }

    private fun evaluateLockState() {
        _lockState.update {
            when {
                biometricChecker.isAvailable() -> LockScreenState.Biometric
                !repository.isPinSetupComplete() -> LockScreenState.PinSetup()
                else -> LockScreenState.PinEntry()
            }
        }
    }

    private fun unlock() {
        _lockState.update { LockScreenState.Unlocked }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.sample.security.AppLockViewModelTest"`
Expected: PASS (12 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/sample/security/LockScreenState.kt app/src/main/java/com/example/sample/security/AppLockViewModel.kt app/src/test/java/com/example/sample/security/AppLockViewModelTest.kt
git commit -m "feat: add AppLockViewModel with biometric/PIN/recovery state machine"
```

---

### Task 8: Koin registration

**Files:**
- Modify: `app/src/main/java/com/example/sample/di/AppModule.kt`

**Interfaces:**
- Consumes: `AppLockRepositoryImpl` (Task 4), `BiometricAvailabilityCheckerImpl` (Task 5), `AppLockViewModel` (Task 7).
- Produces: Koin-resolvable `AppLockRepository`, `BiometricAvailabilityChecker`, `AppLockViewModel` — consumed by `AppLockGate` (Task 10) via `koinViewModel()`.

- [ ] **Step 1: Add the bindings**

In `app/src/main/java/com/example/sample/di/AppModule.kt`, add imports:

```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.sample.security.AppLockViewModel
import com.example.sample.security.BiometricAvailabilityChecker
import com.example.sample.security.BiometricAvailabilityCheckerImpl
import com.example.sample.security.data.AppLockRepository
import com.example.sample.security.data.AppLockRepositoryImpl
```

Add this block before the `// ── ViewModels ──` section:

```kotlin
    // ── App Lock ──────────────────────────────────────────────────────────────

    single(qualifier = org.koin.core.qualifier.named("appLockPrefs")) {
        val context = androidContext()
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "app_lock_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    single<AppLockRepository> {
        AppLockRepositoryImpl(get(qualifier = org.koin.core.qualifier.named("appLockPrefs")))
    }

    single<BiometricAvailabilityChecker> {
        BiometricAvailabilityCheckerImpl(androidContext())
    }
```

Add this line in the `// ── ViewModels ──` section:

```kotlin
    viewModel { AppLockViewModel(get<AppLockRepository>(), get<BiometricAvailabilityChecker>()) }
```

- [ ] **Step 2: Verify it compiles and the app still boots**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/sample/di/AppModule.kt
git commit -m "feat: register AppLock dependencies with Koin"
```

---

### Task 9: `MainActivity` → `FragmentActivity`

**Files:**
- Modify: `app/src/main/java/com/example/sample/MainActivity.kt`

**Interfaces:**
- Produces: `MainActivity` is now an `androidx.fragment.app.FragmentActivity`, required by `androidx.biometric.BiometricPrompt`'s constructor. Consumed by the biometric screen in Task 10.

- [ ] **Step 1: Change the base class**

```kotlin
package com.example.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.sample.navigation.AppNavigation
import com.example.sample.security.AppLockGate
import com.example.sample.ui.theme.SampleTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleTheme {
                AppLockGate {
                    AppNavigation()
                }
            }
        }
    }
}
```

(`AppLockGate` is created in Task 10; this step's edit will not compile until that task lands — that's expected since the two are part of the same reviewable change. Do Task 9 and Task 10 together before running a build.)

- [ ] **Step 2: Commit**

Commit together with Task 10 (see Task 10 Step 6) — do not commit a non-compiling intermediate state.

---

### Task 10: `AppLockGate` and `LockScreen` UI

**Files:**
- Create: `app/src/main/java/com/example/sample/security/AppLockGate.kt`
- Create: `app/src/main/java/com/example/sample/security/ui/LockScreen.kt`
- Create: `app/src/main/java/com/example/sample/security/ui/PinKeypad.kt`
- Create: `app/src/main/java/com/example/sample/security/ui/BiometricUnlockScreen.kt`

**Interfaces:**
- Consumes: `AppLockViewModel`, `LockScreenState`, `LockScreenEvent` (Task 7); `RecoveryQuestion` (Task 3); `koinViewModel()` from `koin-androidx-compose` (already a project dependency).
- Produces: `@Composable fun AppLockGate(content: @Composable () -> Unit)`, used by `MainActivity` (Task 9).

- [ ] **Step 1: Write the shared PIN keypad**

```kotlin
package com.example.sample.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Reusable 0-9 + backspace numeric keypad used by every PIN screen. */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    OutlinedButton(onClick = { onDigit(digit) }, modifier = Modifier.padding(4.dp).size(64.dp)) {
                        Text(digit.toString())
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(64.dp).padding(4.dp))
            OutlinedButton(onClick = { onDigit('0') }, modifier = Modifier.padding(4.dp).size(64.dp)) {
                Text("0")
            }
            OutlinedButton(onClick = onBackspace, modifier = Modifier.padding(4.dp).size(64.dp)) {
                Text("⌫")
            }
        }
    }
}
```

- [ ] **Step 2: Write the biometric screen**

```kotlin
package com.example.sample.security.ui

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricUnlockScreen(
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val activity = LocalContext.current as FragmentActivity
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Sample")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(promptInfo)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Waiting for authentication…")
        Button(onClick = { retryTrigger++ }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Try Again")
        }
    }
}
```

- [ ] **Step 3: Write `LockScreen`**

```kotlin
package com.example.sample.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.security.AppLockViewModel
import com.example.sample.security.LockScreenState
import com.example.sample.security.RecoveryQuestion

@Composable
fun LockScreen(viewModel: AppLockViewModel) {
    val state by viewModel.lockState.collectAsStateWithLifecycle()

    when (val current = state) {
        is LockScreenState.Checking -> Unit

        is LockScreenState.Biometric -> BiometricUnlockScreen(
            onSuccess = viewModel::onBiometricSuccess,
            onError = viewModel::onBiometricFailed,
        )

        is LockScreenState.PinEntry -> PinEntryContent(
            error = current.error,
            onSubmit = viewModel::onPinEntrySubmit,
            onForgotPin = viewModel::onForgotPinClicked,
        )

        is LockScreenState.PinSetup -> PinSetupContent(
            error = current.error,
            onSubmit = viewModel::onPinSetupSubmit,
        )

        is LockScreenState.RecoveryPrompt -> RecoveryQuestionContent(
            questionText = current.question.text,
            error = current.error,
            onSubmit = viewModel::onRecoveryAnswerSubmit,
            onBack = viewModel::onBackToPinEntry,
        )

        is LockScreenState.SetNewPin -> SetNewPinContent(
            error = current.error,
            onSubmit = viewModel::onSetNewPinSubmit,
        )

        is LockScreenState.Unlocked -> Unit
    }
}

@Composable
private fun PinEntryContent(error: String?, onSubmit: (String) -> Unit, onForgotPin: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Enter your PIN")
        Text("● ".repeat(pin.length).trim())
        error?.let { Text(it) }
        PinKeypad(
            onDigit = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    if (pin.length == 4) {
                        onSubmit(pin)
                        pin = ""
                    }
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            modifier = Modifier.padding(top = 16.dp),
        )
        TextButton(onClick = onForgotPin, modifier = Modifier.padding(top = 8.dp)) {
            Text("Forgot PIN?")
        }
    }
}

@Composable
private fun PinSetupContent(
    error: String?,
    onSubmit: (pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(RecoveryQuestion.FIRST_PET) }
    var answer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Set a 4-digit PIN")
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) }, label = { Text("PIN") })
        OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it.filter(Char::isDigit) }, label = { Text("Confirm PIN") })
        Text("Recovery question: ${question.text}")
        RecoveryQuestion.entries.forEach { candidate ->
            TextButton(onClick = { question = candidate }) { Text(candidate.text) }
        }
        OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Your answer") })
        error?.let { Text(it) }
        TextButton(onClick = { onSubmit(pin, confirmPin, question.id, answer) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Save")
        }
    }
}

@Composable
private fun RecoveryQuestionContent(
    questionText: String,
    error: String?,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(questionText)
        OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Your answer") })
        error?.let { Text(it) }
        TextButton(onClick = { onSubmit(answer) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Submit")
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun SetNewPinContent(
    error: String?,
    onSubmit: (pin: String, confirmPin: String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Set a new 4-digit PIN")
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) }, label = { Text("New PIN") })
        OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it.filter(Char::isDigit) }, label = { Text("Confirm PIN") })
        error?.let { Text(it) }
        TextButton(onClick = { onSubmit(pin, confirmPin) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Save")
        }
    }
}
```

- [ ] **Step 4: Write `AppLockGate`**

```kotlin
package com.example.sample.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.security.ui.LockScreen
import org.koin.androidx.compose.koinViewModel

// NOTE: this file is in package com.example.sample.security, so LockScreenState
// below refers to the sealed interface defined in that same package (Task 7) —
// no import needed.

/**
 * Wraps [content] and shows [LockScreen] until the user authenticates, on
 * cold start and whenever the app returns to the foreground after exceeding
 * [AppLockConfig.backgroundLockTimeout].
 */
@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val viewModel: AppLockViewModel = koinViewModel()
    val state by viewModel.lockState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.onAppForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state is LockScreenState.Unlocked) {
        content()
    } else {
        LockScreen(viewModel)
    }
}
```

- [ ] **Step 5: Add the `USE_BIOMETRIC` permission**

In `app/src/main/AndroidManifest.xml`, add before `<application ...>`:

```xml
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

- [ ] **Step 6: Build, fix, and commit together with Task 9**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Fix any compile errors surfaced by the `collectAsState` cleanup note above before proceeding.

```bash
git add app/src/main/java/com/example/sample/MainActivity.kt app/src/main/java/com/example/sample/security/AppLockGate.kt app/src/main/java/com/example/sample/security/ui app/src/main/AndroidManifest.xml
git commit -m "feat: add AppLockGate and lock screen UI, wire into MainActivity"
```

---

### Task 11: Manual verification

**Files:** none (manual QA pass).

**Interfaces:** none — exercises the full stack built in Tasks 1–10.

- [ ] **Step 1: Install and launch on a device/emulator with a device PIN or biometric already enrolled**

Run: `./gradlew :app:installDebug`
Expected: app launches straight into `BiometricUnlockScreen`; completing the OS biometric/device-credential prompt reveals the dashboard.

- [ ] **Step 2: Verify on a device/emulator with NO lock screen set**

Expected: first launch shows the PIN setup screen; after saving a PIN + recovery question + answer, the app unlocks. Force-stop and relaunch: PIN entry screen appears and the saved PIN unlocks it.

- [ ] **Step 3: Verify "Forgot PIN?"**

Expected: tapping it shows the saved recovery question; a wrong answer shows an inline error and allows retry; the right answer leads to Set-New-PIN; saving a new PIN unlocks the app and the old PIN no longer works.

- [ ] **Step 4: Verify the background timeout**

Temporarily set `AppLockConfig.backgroundLockTimeout = 5.seconds` for this manual check only (do not commit), background the app past that, return — the lock screen should reappear. Returning before the timeout should not re-lock. Revert the temporary change (confirm `git diff` is clean) once verified.

- [ ] **Step 5: Record result**

No commit for this task — it is verification only, confirming Tasks 1–10 work end-to-end on-device.
