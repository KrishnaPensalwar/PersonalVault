package com.example.sample.storage.data

import kotlinx.coroutines.flow.Flow

class VaultDocumentImpl(
    private val vaultDocumentDao: VaultDocumentDao
) {

    fun getAllDocuments(): Flow<List<VaultDocumentEntity>> {
        return vaultDocumentDao.getAllDocuments()
    }

    /** Returns only documents belonging to the signed-in user (not linked to any member). */
    fun getMyDocuments(): Flow<List<VaultDocumentEntity>> {
        return vaultDocumentDao.getMyDocuments()
    }

    /** Returns all documents linked to a specific family member. */
    fun getDocumentsByMemberId(memberId: String): Flow<List<VaultDocumentEntity>> {
        return vaultDocumentDao.getDocumentsByMemberId(memberId)
    }

    /** Returns a live count of documents belonging to the given member. */
    fun getDocumentCountByMemberId(memberId: String): Flow<Int> {
        return vaultDocumentDao.getDocumentCountByMemberId(memberId)
    }

    suspend fun saveDocument(
        document: VaultDocumentEntity
    ) {
        vaultDocumentDao.insertDocument(document)
    }

    suspend fun getDocumentById(
        id: String
    ): VaultDocumentEntity? {
        return vaultDocumentDao.getDocumentById(id)
    }

    suspend fun deleteDocumentById(id: String) {
        vaultDocumentDao.deleteDocumentById(id)
    }

    suspend fun deleteDocumentsByMemberId(memberId: String) {
        vaultDocumentDao.deleteDocumentsByMemberId(memberId)
    }

    suspend fun updateDocumentDetails(
        id: String,
        name: String,
    ) {
        vaultDocumentDao.updateDocumentDetails(id, name)
    }
}
