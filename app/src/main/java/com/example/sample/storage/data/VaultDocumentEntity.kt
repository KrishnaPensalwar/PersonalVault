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
    val createdAt: Long = System.currentTimeMillis()
)