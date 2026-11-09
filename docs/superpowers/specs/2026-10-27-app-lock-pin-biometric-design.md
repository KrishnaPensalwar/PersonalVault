# App Lock: PIN / Biometric / Device Credential — Design

## Goal

Gate the app behind an unlock screen on cold start and after being backgrounded
past a configurable threshold. Prefer whatever the device already has
(biometric, PIN, pattern, password); only fall back to an app-managed 4-digit
PIN + recovery question when the device has nothing enrolled.

## Trigger

Lock the app:
- On cold start (process freshly created).
- When returning to foreground after being backgrounded longer than a
  configurable duration.

The threshold is a single named constant, not hardcoded inline, so it can be
changed in one place later:

```kotlin
object AppLockConfig {
    var backgroundLockTimeout: Duration = 5.minutes
}
```

Implemented as a `var` on a small object (not a compile-time constant) so it
can also be wired to a settings screen or DataStore later without touching
call sites — call sites always read `AppLockConfig.backgroundLockTimeout`.

## Architecture

New `security` package:

- `AppLockConfig` — holds `backgroundLockTimeout` (see above).
- `PinHasher` — SHA-256 + random 16-byte salt. Pure functions: `hash(value, salt)`, `generateSalt()`. Unit-testable.
- `AppLockRepository` (interface) / `AppLockRepositoryImpl` — wraps
  `EncryptedSharedPreferences` (new `androidx.security:security-crypto` dep).
  Stores: `pinHash`, `pinSalt`, `recoveryQuestionId`, `recoveryAnswerHash`,
  `recoverySalt`, `isSetupComplete`.
- `AppLockViewModel` — exposes `LockUiState` (sealed): `CheckingAuthMethod`,
  `BiometricPrompting`, `PinEntry`, `PinSetup`, `RecoveryQuestion`,
  `SetNewPin`, `Unlocked`. Decides path via
  `BiometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)`.
- `LockScreen` composable + sub-screens: PIN keypad, PIN setup (PIN + confirm
  + recovery question picker), forgot-PIN recovery flow, set-new-PIN.
- `AppLockGate` composable wrapping `AppNavigation` in `MainActivity`: renders
  `LockScreen` while locked, else real content.
- `AppBackgroundTracker` — uses `ProcessLifecycleOwner` (new
  `androidx.lifecycle:lifecycle-process` dep). Records timestamp on `onStop`;
  on `onStart`, locks if `now - timestamp > AppLockConfig.backgroundLockTimeout`
  or if this is the first foreground of the process.

## Flow

1. App start / foreground-after-timeout → `AppLockGate` marks locked, asks
   `AppLockViewModel` what's needed.
2. Device has biometric or device-credential enrolled → fire `BiometricPrompt`
   with authenticators `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` → OS shows
   fingerprint/face/PIN/pattern/password, whatever is already set up. Success
   → unlock. Cancel/error → stay on lock screen with a retry button.
3. Nothing enrolled on device → app PIN mode:
   - First time (`isSetupComplete == false`) → Setup screen: enter + confirm
     4-digit PIN, pick a recovery question from a fixed preset list of 5,
     enter an answer → hash + save both, set `isSetupComplete = true`.
   - Later → PIN keypad screen. "Forgot PIN?" → show the saved question →
     check answer hash → match → Set-New-PIN screen; mismatch → inline error,
     retry.
4. Unlock clears the in-memory locked flag for the current process; re-locks
   per the trigger rule above.

## Error handling

- Wrong PIN / wrong recovery answer → inline error, no attempt-limit/lockout
  (not requested, avoids over-engineering).
- Biometric error/lockout from the OS → show the OS-provided error text with
  a retry button.

## Testing

- Unit tests for `PinHasher` (hash determinism given same salt, different
  salts differ).
- Unit tests for `AppLockRepository` read/write round-trip (using a fake
  `EncryptedSharedPreferences`-backed test double or in-memory
  `SharedPreferences`).
- Unit tests for `AppLockViewModel` state transitions against a fake
  `AppLockRepository` and injectable biometric-availability check.
- No new instrumentation tests — matches the project's current minimal test
  setup.

## New dependencies

- `androidx.security:security-crypto`
- `androidx.biometric:biometric`
- `androidx.lifecycle:lifecycle-process`

## Out of scope

- Attempt-limiting / lockout timers on wrong PIN.
- Server-side or email-based recovery.
- Per-screen re-lock (e.g. locking just the Password tab) — this is a
  whole-app gate only.
