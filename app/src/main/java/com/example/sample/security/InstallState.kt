package com.example.sample.security

import android.content.Context

/** First-run flag. Cleared only when the app is uninstalled or its data is wiped. */
class InstallState(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun welcomeSeen(): Boolean = prefs.getBoolean(KEY, false)

    fun markWelcomeSeen() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        const val FILE = "install_state"
        const val KEY = "welcome_seen"
    }
}
