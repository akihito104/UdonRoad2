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
import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.TweetDataSource
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.db.dao.ConversationListDao
import com.freshdigitable.udonroad2.data.db.dao.CustomTimelineDao
import com.freshdigitable.udonroad2.data.db.dao.CustomTimelineListDao
import com.freshdigitable.udonroad2.data.db.dao.MediaDao
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.db.dao.ReplyLocalDataSource
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import com.freshdigitable.udonroad2.data.db.dao.UserLocalSource
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import com.freshdigitable.udonroad2.data.local.SharedPreferenceDataSource
import com.freshdigitable.udonroad2.data.local.TweetLocalDataSource
import dagger.Binds
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
    fun provideConversationListDao(
        db: AppDatabase,
        tweetListDao: TweetListDao
    ): ConversationListDao = ConversationListDao(db, tweetListDao)

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
        fun provideReplyRepositoryLocalSource(
            db: AppDatabase
        ): ReplyRepository.LocalSource = ReplyLocalDataSource(db.userReplyDao())

        @Singleton
        @Provides
        fun provideUserRepositoryLocalSource(
            db: AppDatabase
        ): UserDataSource.Local = UserLocalSource(db.userDao())
    }

    @Binds
    fun bindOAuthDataSourceLocal(source: SharedPreferenceDataSource): OAuthTokenDataSource.Local

    @Binds
    fun bindAppSettingDataSourceLocal(
        source: SharedPreferenceDataSource
    ): AppSettingDataSource.Local

    @Binds
    fun bindTweetDataSourceLocal(source: TweetLocalDataSource): TweetDataSource.Local
}
