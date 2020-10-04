package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.restclient.UserRestClient
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import dagger.Module
import dagger.Provides

class UserRepository(
    private val dao: UserDao,
    private val restClient: UserRestClient,
) {
    fun getUserSource(id: UserId): LiveData<out User?> = dao.getUserSourceById(id)

    suspend fun getUser(id: UserId): User? {
        return dao.getUserById(id) ?: restClient.showUser(id).also { dao.addUsers(listOf(it)) }
    }
}

@Module(
    includes = [
        DaoModule::class
    ]
)
object UserRepositoryModule {
    @Provides
    fun provideUserRepository(
        dao: UserDao,
        restClient: UserRestClient,
    ): UserRepository = UserRepository(dao, restClient)
}
