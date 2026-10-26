package com.example.sample.family.data

import kotlinx.coroutines.flow.Flow

/**
 * Concrete Room-backed implementation of [FamilyMemberRepository].
 */
class FamilyMemberRepositoryImpl(
    private val dao: FamilyMemberDao,
) : FamilyMemberRepository {

    override fun getAllMembers(): Flow<List<FamilyMemberEntity>> =
        dao.getAllMembers()

    override suspend fun getMemberById(id: String): FamilyMemberEntity? =
        dao.getMemberById(id)

    override suspend fun saveMember(member: FamilyMemberEntity) {
        dao.insertMember(member)
    }

    override suspend fun updateMember(member: FamilyMemberEntity) {
        dao.updateMember(member)
    }

    override suspend fun deleteMemberById(id: String) {
        dao.deleteMemberById(id)
    }
}
