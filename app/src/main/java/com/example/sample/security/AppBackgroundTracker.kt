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
