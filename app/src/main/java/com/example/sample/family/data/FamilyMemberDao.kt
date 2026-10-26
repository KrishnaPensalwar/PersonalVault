package com.example.sample.family.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    // ── Insert ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMember(member: FamilyMemberEntity)

    // ── Update ──────────────────────────────────────────────────────────────

    @Update
    suspend fun updateMember(member: FamilyMemberEntity)

    // ── Queries ─────────────────────────────────────────────────────────────

    /** Observe all family members ordered by newest first. */
    @Query("SELECT * FROM family_members ORDER BY createdAt DESC")
    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: String): FamilyMemberEntity?

    // ── Delete ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteMemberById(id: String)
}
