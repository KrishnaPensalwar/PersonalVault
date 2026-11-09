package com.example.sample.password.data

import com.example.sample.security.VaultSession
import com.example.sample.security.opened
import com.example.sample.security.sealed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PasswordRepositoryImpl(
    private val dao: PasswordDao,
) : PasswordRepository {

    override fun getAllPasswords(): Flow<List<PasswordEntity>> =
        combine(dao.getAllPasswords(), VaultSession.open) { rows, open ->
            if (!open) emptyList() else rows.map { it.opened() }
        }

    override fun getFavoritePasswords(): Flow<List<PasswordEntity>> =
        combine(dao.getFavoritePasswords(), VaultSession.open) { rows, open ->
            if (!open) emptyList() else rows.map { it.opened() }
        }

    override fun searchPasswords(query: String): Flow<List<PasswordEntity>> =
        combine(dao.getAllPasswords(), VaultSession.open) { rows, open ->
            if (!open) emptyList()
            else rows.map { it.opened() }.filter { row ->
                row.title.contains(query, ignoreCase = true) ||
                    row.username.contains(query, ignoreCase = true) ||
                    row.websiteUrl.contains(query, ignoreCase = true)
            }
        }

    override suspend fun getPasswordById(id: String): PasswordEntity? {
        if (!VaultSession.isOpen()) return null
        return dao.getPasswordById(id)?.opened()
    }

    override suspend fun savePassword(password: PasswordEntity) {
        dao.insertPassword(password.sealed())
    }

    override suspend fun updatePassword(password: PasswordEntity) {
        dao.updatePassword(password.sealed())
    }

    override suspend fun deletePasswordById(id: String) {
        dao.deletePasswordById(id)
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite)
    }
}
