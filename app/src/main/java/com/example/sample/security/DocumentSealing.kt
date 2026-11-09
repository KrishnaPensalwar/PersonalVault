package com.example.sample.security

import java.io.File

/**
 * Vault documents are AES-GCM with a 4-byte header. Legacy plaintext files have no header
 * and stay readable until [migrateFile] replaces them via a temp file.
 */
object DocumentSealing {

    private val MAGIC = byteArrayOf('P'.code.toByte(), 'V'.code.toByte(), '1'.code.toByte(), 0)

    fun isEncrypted(file: File): Boolean {
        if (file.length() < MAGIC.size) return false
        val header = ByteArray(MAGIC.size)
        file.inputStream().use { it.read(header) }
        return header.contentEquals(MAGIC)
    }

    fun write(file: File, plaintext: ByteArray) {
        val payload = VaultCrypto.encrypt(VaultSession.require(), plaintext)
        val tmp = File(file.parentFile, file.name + ".migrating")
        tmp.outputStream().use { out ->
            out.write(MAGIC)
            out.write(payload)
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    fun read(file: File): ByteArray {
        val bytes = file.readBytes()
        if (bytes.size < MAGIC.size || !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            return bytes
        }
        return VaultCrypto.decrypt(VaultSession.require(), bytes.copyOfRange(MAGIC.size, bytes.size))
    }

    /**
     * Encrypts a legacy plaintext file in place. An existing encrypted file is left untouched.
     * A crash before rename leaves the original plaintext and a `.migrating` file, which is deleted
     * on the next attempt.
     */
    fun migrateFile(file: File) {
        if (!file.isFile || isEncrypted(file)) {
            File(file.parentFile, file.name + ".migrating").delete()
            return
        }
        write(file, file.readBytes())
    }

    fun decryptedCopy(cacheDir: File, source: File): File {
        val directory = File(cacheDir, "vault-open").apply { mkdirs() }
        val out = File(directory, source.name)
        out.writeBytes(read(source))
        return out
    }
}
