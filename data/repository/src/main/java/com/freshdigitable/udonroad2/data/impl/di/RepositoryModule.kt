/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.impl.di

import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.TweetDataSource
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.LocalSourceModule
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.ReplyRepositoryImpl
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.data.local.SharedPreferenceDataSource
import com.freshdigitable.udonroad2.data.restclient.FriendshipRestClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    includes = [
        DaoModule::class,
        LocalSourceModule::class,
    ]
)
interface RepositoryModule {
    companion object {
        @Provides
        fun provideTweetRepository(
            local: TweetDataSource.Local,
            remote: TweetDataSource.Remote,
        ): TweetRepository = TweetRepository(local, remote)

        @Provides
        fun provideUserRepository(
            localSource: UserDataSource.Local,
            restClient: UserDataSource.Remote,
        ): UserDataSource = UserRepository(localSource, restClient)

        @Provides
        fun provideRelationshipRepository(
            dao: RelationshipDao,
            prefs: SharedPreferenceDataSource,
            restClient: FriendshipRestClient,
        ): RelationshipRepository = RelationshipRepository(dao, prefs, restClient)

        @Provides
        fun provideOAuthTokenRepository(
            prefs: OAuthTokenDataSource.Local,
            apiClient: OAuthTokenDataSource.Remote,
        ): OAuthTokenDataSource = OAuthTokenRepository(prefs, apiClient)

        @Provides
        @Singleton
        fun provideReplyRepository(localSource: ReplyRepository.LocalSource): ReplyRepository =
            ReplyRepositoryImpl(localSource)
    }

    @Binds
    fun bindAppSettingDataSource(source: AppSettingRepository): AppSettingDataSource
}
