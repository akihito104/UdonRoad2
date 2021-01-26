package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.restclient.UserRestClient
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val dao: UserDao,
    private val restClient: UserRestClient,
) {
    fun getUserFlow(id: UserId): Flow<UserEntity?> = dao.getUserFlowById(id)

    suspend fun getUser(id: UserId): UserEntity {
        return dao.getUserById(id) ?: restClient.showUser(id).also { dao.addUsers(listOf(it)) }
    }

    suspend fun addUser(user: UserEntity) {
        dao.addUsers(listOf(user))
    }
}
