package com.example.sample.password.data

import kotlinx.coroutines.flow.Flow

/**
 * Concrete Room-backed implementation of [PasswordRepository].
 *
 * Phase 2 migration path:
 *   1. Inject an `EncryptionService` here.
 *   2. Wrap [savePassword] / [updatePassword] to encrypt sensitive fields before
 *      passing them to the DAO.
 *   3. Wrap [getAllPasswords], [searchPasswords], etc. with `.map { list -> list.decrypt() }`.
 *   4. No ViewModel changes required.
 */
class PasswordRepositoryImpl(
    private val dao: PasswordDao,
) : PasswordRepository {

    override fun getAllPasswords(): Flow<List<PasswordEntity>> =
        dao.getAllPasswords()

    override fun getFavoritePasswords(): Flow<List<PasswordEntity>> =
        dao.getFavoritePasswords()

    override fun searchPasswords(query: String): Flow<List<PasswordEntity>> =
        dao.searchPasswords(query)

    override suspend fun getPasswordById(id: String): PasswordEntity? =
        dao.getPasswordById(id)

    override suspend fun savePassword(password: PasswordEntity) {
        dao.insertPassword(password)
    }

    override suspend fun updatePassword(password: PasswordEntity) {
        dao.updatePassword(password)
    }

    override suspend fun deletePasswordById(id: String) {
        dao.deletePasswordById(id)
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite)
    }
}
