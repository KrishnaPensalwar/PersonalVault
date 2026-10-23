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

    @Query("SELECT * FROM vault_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(
        id: String
    ): VaultDocumentEntity?

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

}