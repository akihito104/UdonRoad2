package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow

internal class UserRepository(
    private val localSource: UserDataSource.Local,
    private val restClient: UserDataSource.Remote,
) : UserDataSource {
    override fun getUserSource(id: UserId): Flow<UserEntity?> = localSource.getUserSource(id)

    override suspend fun findUser(id: UserId): UserEntity? = localSource.findUser(id)

    override suspend fun getUser(id: UserId): UserEntity {
        val cache = localSource.findUser(id)
        if (cache != null) {
            return cache
        }
        val res = restClient.getUser(id)
        addUser(res)
        return res
    }

    override suspend fun addUser(user: UserEntity) {
        localSource.addUser(user)
    }
}
