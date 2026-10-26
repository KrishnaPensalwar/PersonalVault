package com.example.sample.family.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a family member in the vault.
 */
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String,

    /** Display name of the family member */
    val name: String,

    /** Relationship label, e.g. "Spouse", "Father", "Son" */
    val relationship: String,

    /** Unix epoch milliseconds – set once at creation */
    val createdAt: Long = System.currentTimeMillis(),

    /** Unix epoch milliseconds – updated on every save */
    val updatedAt: Long = System.currentTimeMillis(),
)
