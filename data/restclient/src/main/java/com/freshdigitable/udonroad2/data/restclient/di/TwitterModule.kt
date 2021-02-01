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

package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.RelationDataSource
import com.freshdigitable.udonroad2.data.TweetDataSource
import com.freshdigitable.udonroad2.data.UserDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import javax.inject.Singleton

@Module
object TwitterModule {
    @Provides
    @Singleton
    fun providesTwitter(): Twitter {
        val configuration = ConfigurationBuilder()
            .setTweetModeExtended(true)
            .setOAuthConsumerKey(BuildConfig.CONSUMER_KEY)
            .setOAuthConsumerSecret(BuildConfig.CONSUMER_SECRET)
            .build()
        return TwitterFactory(configuration).instance
    }
}

@Module
interface AppTwitterModule {
    companion object {
        @Provides
        @Singleton
        fun providesAppTwitter(twitter: Twitter): AppTwitter = AppTwitter(twitter)

        @Provides
        @Singleton
        fun provideUserRemoteSource(appTwitter: AppTwitter): UserDataSource.Remote =
            UserRestClient(appTwitter)
    }

    @Binds
    fun bindOAuthDataSourceRemote(source: OAuthApiClient): OAuthTokenDataSource.Remote

    @Binds
    fun bindAppSettingDataSourceRemote(source: OAuthApiClient): AppSettingDataSource.Remote

    @Binds
    fun bindTweetDataSourceRemote(source: TweetApiClient): TweetDataSource.Remote

    @Binds
    fun bindRelationshipDataSourceRemote(source: FriendshipRestClient): RelationDataSource.Remote
}
