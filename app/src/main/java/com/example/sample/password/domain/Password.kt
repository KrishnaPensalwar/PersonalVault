package com.example.sample.password.domain

/**
 * Domain / UI model for a password entry.
 *
 * This is intentionally separate from [PasswordEntity] so that:
 *  - The UI never directly depends on the Room layer.
 *  - Phase 2 decryption can happen in the repository mapping layer before
 *    this object is constructed.
 */
data class Password(
    val id: String,
    val title: String,
    val username: String,
    /** In Phase 2 this will arrive pre-decrypted from the repository. */
    val password: String,
    val websiteUrl: String,
    val notes: String,
    val category: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Pre-defined categories available in the UI. */
val PasswordCategories = listOf(
    "All",
    "Social",
    "Work",
    "Finance",
    "Shopping",
    "Entertainment",
    "Health",
    "Education",
    "Other",
)
