package com.freshdigitable.udonroad2.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.model.RepositoryScope
import com.freshdigitable.udonroad2.model.User
import dagger.Module
import dagger.Provides

class UserRepository(
    private val dao: UserDao
) {
    fun getUser(
        id: Long
    ): LiveData<User?> = dao.getUser(id).map { it }
}

@Module(
    includes = [
        DaoModule::class
    ]
)
object UserRepositoryModule {
    @Provides
    @RepositoryScope
    @JvmStatic
    fun provideUserRepository(dao: UserDao): UserRepository {
        return UserRepository(dao)
    }
}
