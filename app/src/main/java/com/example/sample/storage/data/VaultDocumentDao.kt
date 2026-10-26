package com.example.sample.storage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDocumentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDocument(
        document: VaultDocumentEntity
    )

    @Query("SELECT * FROM vault_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<VaultDocumentEntity>>

    /** Returns only documents that belong to the user (not linked to any family member). */
    @Query("SELECT * FROM vault_documents WHERE familyMemberId IS NULL ORDER BY createdAt DESC")
    fun getMyDocuments(): Flow<List<VaultDocumentEntity>>

    /** Returns all documents linked to a specific family member. */
    @Query("SELECT * FROM vault_documents WHERE familyMemberId = :memberId ORDER BY createdAt DESC")
    fun getDocumentsByMemberId(memberId: String): Flow<List<VaultDocumentEntity>>

    /** Returns the count of documents linked to a specific family member. */
    @Query("SELECT COUNT(*) FROM vault_documents WHERE familyMemberId = :memberId")
    fun getDocumentCountByMemberId(memberId: String): Flow<Int>

    @Query("SELECT * FROM vault_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(
        id: String
    ): VaultDocumentEntity?

    @Query("UPDATE vault_documents SET name = :name WHERE id = :id")
    suspend fun updateDocumentDetails(
        id: String,
        name: String,
    )

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM vault_documents WHERE familyMemberId = :memberId")
    suspend fun deleteDocumentsByMemberId(memberId: String)
}