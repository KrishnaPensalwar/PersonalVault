package com.example.sample.password.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a stored password entry.
 *
 * Phase 2 note: The [title], [username], [password], and [notes] fields will be
 * replaced with encrypted byte arrays (via AES-GCM / EncryptedSharedPreferences /
 * Jetpack Security) when encryption is integrated. The schema version will be bumped
 * and a migration will be provided. No other structural changes are expected.
 */
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey
    val id: String,

    /** Human-readable label, e.g. "GitHub" */
    val title: String,

    /** Login username / e-mail associated with the entry */
    val username: String,

    /**
     * Plaintext password – stored as-is in Phase 1.
     * Phase 2: replace with an encrypted blob (Base64 string or ByteArray column).
     */
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
