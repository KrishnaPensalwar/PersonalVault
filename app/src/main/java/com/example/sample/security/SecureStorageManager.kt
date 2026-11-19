package com.example.sample.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_vault_prefs")

interface SecureStorageManager {
    fun storeString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun storeInt(key: String, value: Int)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun storeLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun storeBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun contains(key: String): Boolean
    fun remove(key: String)
}

class SecureStorageManagerImpl(private val context: Context) : SecureStorageManager {

    private val keyAlias = "SecureStorageKey"
    private val androidKeystore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"

    private val dataStore = context.secureDataStore

    init {
        generateKeyIfNotExists()
        migrateFromEncryptedSharedPreferences()
    }

    private fun generateKeyIfNotExists() {
        val keyStore = KeyStore.getInstance(androidKeystore).apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeystore)
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(androidKeystore).apply { load(null) }
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        return "$ivBase64]$encryptedBase64"
    }

    private fun decrypt(encryptedData: String): String {
        val parts = encryptedData.split("]")
        if (parts.size != 2) return encryptedData // Fallback if data isn't encrypted properly
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decrypted = cipher.doFinal(cipherText)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedData // Return original if decryption fails (e.g. key invalidated)
        }
    }

    override fun storeString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        val encrypted = encrypt(value)
        runBlocking {
            dataStore.edit { prefs ->
                prefs[prefKey] = encrypted
            }
        }
    }

    override fun getString(key: String, defaultValue: String?): String? {
        val prefKey = stringPreferencesKey(key)
        val value = runBlocking { dataStore.data.first()[prefKey] }
        return if (value != null) {
            decrypt(value)
        } else {
            defaultValue
        }
    }

    override fun storeInt(key: String, value: Int) {
        storeString(key, value.toString())
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        val str = getString(key)
        return str?.toIntOrNull() ?: defaultValue
    }

    override fun storeLong(key: String, value: Long) {
        storeString(key, value.toString())
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        val str = getString(key)
        return str?.toLongOrNull() ?: defaultValue
    }

    override fun storeBoolean(key: String, value: Boolean) {
        storeString(key, value.toString())
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val str = getString(key)
        return str?.toBooleanStrictOrNull() ?: defaultValue
    }

    override fun contains(key: String): Boolean {
        val prefKey = stringPreferencesKey(key)
        return runBlocking { dataStore.data.first().contains(prefKey) }
    }

    override fun remove(key: String) {
        val prefKey = stringPreferencesKey(key)
        runBlocking {
            dataStore.edit { prefs ->
                prefs.remove(prefKey)
            }
        }
    }

    private fun migrateFromEncryptedSharedPreferences() {
        // Only run migration once if not already migrated
        val migratedKey = stringPreferencesKey("migration_complete")
        val isMigrated = runBlocking { dataStore.data.first()[migratedKey] }
        if (isMigrated == "true") return

        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPrefs = EncryptedSharedPreferences.create(
                context,
                "app_lock_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val allEntries = sharedPrefs.all
            if (allEntries.isNotEmpty()) {
                runBlocking {
                    dataStore.edit { prefs ->
                        allEntries.forEach { (key, value) ->
                            val stringValue = value.toString()
                            prefs[stringPreferencesKey(key)] = encrypt(stringValue)
                        }
                        prefs[migratedKey] = "true"
                    }
                }
                // Clear out the old shared preferences after successful migration
                sharedPrefs.edit().clear().apply()
                // Can also delete the file if needed: context.deleteSharedPreferences("app_lock_prefs")
            } else {
                // If it's empty, just mark as migrated
                runBlocking {
                    dataStore.edit { prefs ->
                        prefs[migratedKey] = "true"
                    }
                }
            }
        } catch (e: Exception) {
            // Log error, possibly the old key was corrupted/lost, so proceed with fresh store
        }
    }
}
