package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import dagger.Module
import dagger.Provides

class UserRepository(
    private val dao: UserDao
) {
    fun getUser(id: UserId): LiveData<User?> = dao.getUserById(id)
}

@Module(
    includes = [
        DaoModule::class
    ]
)
object UserRepositoryModule {
    @Provides
    fun provideUserRepository(dao: UserDao): UserRepository {
        return UserRepository(dao)
    }
}
