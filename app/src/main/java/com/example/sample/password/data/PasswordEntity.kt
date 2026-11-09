package com.example.sample.password.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a password entry. String fields are AES-GCM ciphertext (`enc1:` + Base64)
 * once the vault key is installed. The columns stay TEXT so existing rows can be sealed in place.
 */
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey
    val id: String,

    /** Human-readable label, e.g. "GitHub" */
    val title: String,

    /** Login username / e-mail associated with the entry */
    val username: String,

    /** Secret, stored sealed. */
    val password: String,

    /** Optional URL of the service, e.g. "https://github.com" */
    val websiteUrl: String,

    /** Optional additional notes */
    val notes: String,

    /** Category tag for grouping, e.g. "Social", "Work", "Finance" */
    val category: String,

    /** Whether the user has starred this entry */
    val isFavorite: Boolean = false,

    /** Unix epoch milliseconds – set once at creation */
    val createdAt: Long = System.currentTimeMillis(),

    /** Unix epoch milliseconds – updated on every save */
    val updatedAt: Long = System.currentTimeMillis(),
)
