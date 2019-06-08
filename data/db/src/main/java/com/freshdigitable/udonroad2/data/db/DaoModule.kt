/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2.data.db

import android.app.Application
import androidx.room.Room
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object DatabaseModule {
    @Provides
    @JvmStatic
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
}

@Module
object DaoModule {
    @Provides
    @JvmStatic
    fun providesTweetDao(db: AppDatabase): TweetDao = db.tweetDao()

    @Provides
    @JvmStatic
    fun providesUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    @JvmStatic
    fun provideTweetListDao(dao: TweetDao): TweetListDao = TweetListDao(dao)

    @Provides
    @JvmStatic
    fun provideUserListDao(dao: UserDao): UserListDao = UserListDao(dao)
}
