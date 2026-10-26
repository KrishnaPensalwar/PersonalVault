package com.example.sample.storage.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val relativePath: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * If non-null, this document belongs to the specified family member.
     * Null means it belongs to the user's own storage (shown in the Storage tab).
     */
    val familyMemberId: String? = null,
)