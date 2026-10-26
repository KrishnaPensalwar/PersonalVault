package com.example.sample.family.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for family member CRUD operations.
 */
interface FamilyMemberRepository {

    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    suspend fun getMemberById(id: String): FamilyMemberEntity?

    suspend fun saveMember(member: FamilyMemberEntity)

    suspend fun updateMember(member: FamilyMemberEntity)

    suspend fun deleteMemberById(id: String)
}
