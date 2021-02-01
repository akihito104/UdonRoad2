/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.auth.AccessToken

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
        Timber.tag("AppTwitter").d("fetch: $block")
        try {
            block(twitter)
        } catch (ex: TwitterException) {
            throw AppTwitterException.create(ex)
        }
    }
}
