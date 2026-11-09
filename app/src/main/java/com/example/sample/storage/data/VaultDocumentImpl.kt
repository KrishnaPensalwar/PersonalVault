package com.example.sample.storage.data

import com.example.sample.security.VaultCrypto
import com.example.sample.security.VaultSession
import com.example.sample.security.opened
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class VaultDocumentImpl(
    private val vaultDocumentDao: VaultDocumentDao
) {

    fun getAllDocuments(): Flow<List<VaultDocumentEntity>> = open(vaultDocumentDao.getAllDocuments())

    fun getMyDocuments(): Flow<List<VaultDocumentEntity>> = open(vaultDocumentDao.getMyDocuments())

    fun getDocumentsByMemberId(memberId: String): Flow<List<VaultDocumentEntity>> =
        open(vaultDocumentDao.getDocumentsByMemberId(memberId))

    fun getDocumentCountByMemberId(memberId: String): Flow<Int> =
        vaultDocumentDao.getDocumentCountByMemberId(memberId)

    suspend fun saveDocument(document: VaultDocumentEntity) {
        vaultDocumentDao.insertDocument(document.sealedLabels())
    }

    suspend fun getDocumentById(id: String): VaultDocumentEntity? {
        if (!VaultSession.isOpen()) return null
        return vaultDocumentDao.getDocumentById(id)?.opened()
    }

    suspend fun deleteDocumentById(id: String) {
        vaultDocumentDao.deleteDocumentById(id)
    }

    suspend fun deleteDocumentsByMemberId(memberId: String) {
        vaultDocumentDao.deleteDocumentsByMemberId(memberId)
    }

    suspend fun updateDocumentDetails(id: String, name: String) {
        val existing = vaultDocumentDao.getDocumentById(id)
        vaultDocumentDao.updateLabels(
            id,
            VaultCrypto.sealString(VaultSession.require(), name),
            existing?.category ?: "",
        )
    }

    private fun open(source: Flow<List<VaultDocumentEntity>>): Flow<List<VaultDocumentEntity>> =
        combine(source, VaultSession.open) { rows, unlocked ->
            if (!unlocked) emptyList() else rows.map { it.opened() }
        }
}

private fun VaultDocumentEntity.sealedLabels(): VaultDocumentEntity {
    val key = VaultSession.require()
    return copy(
        name = if (VaultCrypto.isSealed(name)) name else VaultCrypto.sealString(key, name),
        category = if (VaultCrypto.isSealed(category)) category else VaultCrypto.sealString(key, category),
    )
}
