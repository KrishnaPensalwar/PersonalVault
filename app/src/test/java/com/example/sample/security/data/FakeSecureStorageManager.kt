package com.example.sample.security.data

import com.example.sample.security.SecureStorageManager

/** In-memory [SecureStorageManager] fake for JVM unit tests. */
class FakeSecureStorageManager : SecureStorageManager {
    private val values = mutableMapOf<String, Any?>()

    override fun storeString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return values[key] as? String ?: defaultValue
    }

    override fun storeInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return values[key] as? Int ?: defaultValue
    }

    override fun storeLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return values[key] as? Long ?: defaultValue
    }

    override fun storeBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defaultValue
    }

    override fun contains(key: String): Boolean {
        return values.containsKey(key)
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
