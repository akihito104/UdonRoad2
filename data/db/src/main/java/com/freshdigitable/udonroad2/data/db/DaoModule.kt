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
import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.db.dao.ConversationListDao
import com.freshdigitable.udonroad2.data.db.dao.CustomTimelineDao
import com.freshdigitable.udonroad2.data.db.dao.CustomTimelineListDao
import com.freshdigitable.udonroad2.data.db.dao.MediaDao
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.db.dao.ReplyLocalDataSource
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
}

@Module
object DaoModule {
    @Provides
    fun providesTweetDao(db: AppDatabase): TweetDao = db.tweetDao()

    @Provides
    fun provideTweetListDao(db: AppDatabase): TweetListDao = TweetListDao(db)

    @Provides
    fun provideConversationListDao(db: AppDatabase): ConversationListDao = ConversationListDao(db)

    @Provides
    fun providesUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideUserListDao(db: AppDatabase): UserListDao = UserListDao(db)

    @Provides
    fun provideCustomTimelineDao(db: AppDatabase): CustomTimelineDao = db.customTimelineDao()

    @Provides
    fun provideCustomTimelineListDao(db: AppDatabase): CustomTimelineListDao =
        CustomTimelineListDao(db)

    @Provides
    fun provideRelationshipDao(db: AppDatabase): RelationshipDao = db.relationshipDao()

    @Provides
    fun provideMediaDao(db: AppDatabase): MediaDao = db.mediaDao()

    @Provides
    fun provideListDao(db: AppDatabase): ListDao = db.listDao()
}

@Module
interface LocalSourceModule {
    companion object {
        @Singleton
        @Provides
        fun bindReplyRepositoryLocalSource(
            db: AppDatabase
        ): ReplyRepository.LocalSource = ReplyLocalDataSource(db.userReplyDao())
    }
}
