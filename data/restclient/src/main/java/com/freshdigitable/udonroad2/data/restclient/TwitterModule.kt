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

import com.freshdigitable.udonroad2.model.AccessTokenEntity
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
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
object AppTwitterModule {
    @Provides
    @Singleton
    fun providesAppTwitter(twitter: Twitter): AppTwitter = AppTwitter(twitter)
}

class AppTwitter(
    private val twitter: Twitter
) {
    var oauthToken: AccessTokenEntity? = null
        set(value) {
            twitter.oAuthAccessToken = when (value) {
                null -> null
                else -> AccessToken(value.token, value.tokenSecret)
            }
            field = value
        }

    suspend fun <T> fetch(block: Twitter.() -> T): T = withContext(Dispatchers.IO) {
        try {
            block(twitter)
        } catch (ex: TwitterException) {
            throw AppTwitterException(ex)
        }
    }
}
