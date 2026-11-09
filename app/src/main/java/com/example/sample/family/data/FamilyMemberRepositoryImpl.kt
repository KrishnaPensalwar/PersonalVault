package com.example.sample.family.data

import com.example.sample.security.VaultSession
import com.example.sample.security.opened
import com.example.sample.security.sealed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FamilyMemberRepositoryImpl(
    private val dao: FamilyMemberDao,
) : FamilyMemberRepository {

    override fun getAllMembers(): Flow<List<FamilyMemberEntity>> =
        combine(dao.getAllMembers(), VaultSession.open) { rows, open ->
            if (!open) emptyList() else rows.map { it.opened() }
        }

    override suspend fun getMemberById(id: String): FamilyMemberEntity? {
        if (!VaultSession.isOpen()) return null
        return dao.getMemberById(id)?.opened()
    }

    override suspend fun saveMember(member: FamilyMemberEntity) {
        dao.insertMember(member.sealed())
    }

    override suspend fun updateMember(member: FamilyMemberEntity) {
        dao.updateMember(member.sealed())
    }

    override suspend fun deleteMemberById(id: String) {
        dao.deleteMemberById(id)
    }
}
