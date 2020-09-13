package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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
    private val appExecutor: AppExecutor,
) {
    fun getUser(id: UserId): LiveData<User?> {
        return MediatorLiveData<User?>().apply {
            addSource(dao.getUserById(id)) { u ->
                when {
                    u != null -> this.value = u
                    else -> fetchUser(id)
                }
            }
        }
    }

    private fun fetchUser(id: UserId) {
        appExecutor.launchIO {
            val user = restClient.showUser(id)
            dao.addUsers(listOf(user))
        }
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
        appExecutor: AppExecutor
    ): UserRepository {
        return UserRepository(dao, restClient, appExecutor)
    }
}
