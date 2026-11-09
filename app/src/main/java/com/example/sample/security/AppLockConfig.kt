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
