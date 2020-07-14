/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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
import com.freshdigitable.udonroad2.model.RequestTokenItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import javax.inject.Inject

class OAuthApiClient @Inject constructor(
    private val twitter: Twitter
) {
    fun login(oauthAccessToken: AccessTokenEntity) {
        twitter.oAuthAccessToken = AccessToken(oauthAccessToken.token, oauthAccessToken.tokenSecret)
    }

    fun logout() {
        twitter.oAuthAccessToken = null
    }

    suspend fun getRequestToken(): RequestTokenItem = withContext(Dispatchers.IO) {
        twitter.getOAuthRequestToken("oob").toItem()
    }

    suspend fun getOauthAccessToken(
        requestToken: RequestTokenItem,
        verifier: String
    ): AccessTokenEntity = withContext(Dispatchers.IO) {
        val token: RequestToken = (requestToken as RequestTokenItemImpl).token
        twitter.getOAuthAccessToken(token, verifier).toEntity()
    }

    private fun RequestToken.toItem(): RequestTokenItem = RequestTokenItemImpl(this)

    private fun AccessToken.toEntity(): AccessTokenEntity =
        AccessTokenEntity.create(userId, token, tokenSecret)
}

private data class RequestTokenItemImpl(override val token: RequestToken) : RequestTokenItem {
    override val authorizationUrl: String = token.authorizationURL
}
