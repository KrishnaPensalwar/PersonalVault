package com.example.sample.storage.data

import kotlinx.coroutines.flow.Flow

class VaultDocumentImpl(
    private val vaultDocumentDao: VaultDocumentDao
) {

    fun getAllDocuments(): Flow<List<VaultDocumentEntity>> {
        return vaultDocumentDao.getAllDocuments()
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
}
