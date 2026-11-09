package com.example.sample.security

import com.example.sample.family.data.FamilyMemberDao
import com.example.sample.password.data.PasswordDao
import com.example.sample.password.data.PasswordEntity
import com.example.sample.storage.data.VaultDocumentDao
import com.example.sample.storage.data.VaultDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Rewrites plaintext rows and document files with AES-GCM after the data key is installed.
 * Already sealed rows and files are skipped, so a second run or a retry after a crash is safe.
 */
class VaultMigrator(
    private val passwords: PasswordDao,
    private val family: FamilyMemberDao,
    private val documents: VaultDocumentDao,
    private val filesDir: File,
) {
    fun migrateNow() {
        if (!VaultSession.isOpen()) return
        runBlocking(Dispatchers.IO) {
            passwords.listAll().filterNot { it.isSealed() }.forEach { row ->
                passwords.updatePassword(row.sealed())
            }
            family.listAll().filterNot { it.isSealed() }.forEach { row ->
                family.updateMember(row.sealed())
            }
            documents.listAll().filterNot { it.isSealed() }.forEach { row ->
                documents.updateLabels(row.id, seal(row.name), seal(row.category))
            }
            val directory = File(filesDir, "vault/documents")
            directory.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.endsWith(".migrating")) DocumentSealing.migrateFile(file)
            }
        }
    }
}

internal fun PasswordEntity.isSealed(): Boolean = VaultCrypto.isSealed(password)

internal fun PasswordEntity.sealed(): PasswordEntity {
    val key = VaultSession.require()
    return copy(
        title = seal(title, key),
        username = seal(username, key),
        password = seal(password, key),
        websiteUrl = seal(websiteUrl, key),
        notes = seal(notes, key),
        category = seal(category, key),
    )
}

internal fun PasswordEntity.opened(): PasswordEntity {
    if (!VaultSession.isOpen()) return this
    val key = VaultSession.require()
    return copy(
        title = open(title, key),
        username = open(username, key),
        password = open(password, key),
        websiteUrl = open(websiteUrl, key),
        notes = open(notes, key),
        category = open(category, key),
    )
}

internal fun com.example.sample.family.data.FamilyMemberEntity.isSealed(): Boolean =
    VaultCrypto.isSealed(name)

internal fun com.example.sample.family.data.FamilyMemberEntity.sealed(): com.example.sample.family.data.FamilyMemberEntity {
    val key = VaultSession.require()
    return copy(name = seal(name, key), relationship = seal(relationship, key))
}

internal fun com.example.sample.family.data.FamilyMemberEntity.opened(): com.example.sample.family.data.FamilyMemberEntity {
    if (!VaultSession.isOpen()) return this
    val key = VaultSession.require()
    return copy(name = open(name, key), relationship = open(relationship, key))
}

internal fun VaultDocumentEntity.isSealed(): Boolean = VaultCrypto.isSealed(name)

internal fun VaultDocumentEntity.opened(): VaultDocumentEntity {
    if (!VaultSession.isOpen()) return this
    val key = VaultSession.require()
    return copy(name = open(name, key), category = open(category, key))
}

private fun seal(value: String, key: ByteArray = VaultSession.require()): String =
    if (VaultCrypto.isSealed(value)) value else VaultCrypto.sealString(key, value)

private fun open(value: String, key: ByteArray): String =
    if (VaultCrypto.isSealed(value)) VaultCrypto.openString(key, value) else value
