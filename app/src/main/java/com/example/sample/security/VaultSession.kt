package com.example.sample.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * In-memory data key. Cleared when the vault locks. Not written to disk.
 */
object VaultSession {

    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    @Volatile
    private var dek: ByteArray? = null

    @Volatile
    var cacheDir: File? = null

    fun isOpen(): Boolean = dek != null

    fun install(key: ByteArray) {
        dek?.fill(0)
        dek = key.copyOf()
        _open.value = true
    }

    fun require(): ByteArray = dek?.copyOf() ?: error("Vault is locked")

    fun clear() {
        dek?.fill(0)
        dek = null
        _open.value = false
        cacheDir?.let { root ->
            File(root, "vault-open").deleteRecursively()
            File(root, "vault-share").deleteRecursively()
        }
    }
}
