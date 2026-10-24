package com.example.sample.password.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for password storage.
 *
 * Abstracting behind an interface makes it easy to swap implementations in
 * Phase 2 (e.g., wrapping the DAO calls with encryption/decryption logic)
 * without touching the ViewModel.
 */
interface PasswordRepository {

    fun getAllPasswords(): Flow<List<PasswordEntity>>

    fun getFavoritePasswords(): Flow<List<PasswordEntity>>

    /**
     * Returns passwords whose title, username, or websiteUrl contain [query].
     * An empty query returns all passwords.
     */
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    suspend fun getPasswordById(id: String): PasswordEntity?

    suspend fun savePassword(password: PasswordEntity)

    suspend fun updatePassword(password: PasswordEntity)

    suspend fun deletePasswordById(id: String)

    suspend fun toggleFavorite(id: String, isFavorite: Boolean)
}
