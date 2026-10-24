package com.example.sample.password.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    // ── Insert ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPassword(password: PasswordEntity)

    // ── Update ──────────────────────────────────────────────────────────────

    @Update
    suspend fun updatePassword(password: PasswordEntity)

    // ── Queries ─────────────────────────────────────────────────────────────

    /** Observe all passwords ordered by newest first. */
    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    /** Observe only favourite passwords ordered by newest first. */
    @Query("SELECT * FROM passwords WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePasswords(): Flow<List<PasswordEntity>>

    /**
     * Full-text search across title, username, and websiteUrl.
     * Room will rerun this query whenever the underlying table changes.
     */
    @Query(
        """
        SELECT * FROM passwords
        WHERE title LIKE '%' || :query || '%'
           OR username LIKE '%' || :query || '%'
           OR websiteUrl LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE id = :id LIMIT 1")
    suspend fun getPasswordById(id: String): PasswordEntity?

    // ── Delete ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePasswordById(id: String)

    // ── Toggle favourite ─────────────────────────────────────────────────────

    @Query("UPDATE passwords SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())
}
