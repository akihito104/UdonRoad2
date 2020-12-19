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

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserId
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import javax.inject.Inject

class OAuthApiClient @Inject constructor(
    private val twitter: AppTwitter
) {
    suspend fun login(oauthAccessToken: AccessTokenEntity): UserEntity {
        twitter.oauthToken = oauthAccessToken
        return twitter.fetch { verifyCredentials().toEntity() }
    }

    fun logout() {
        twitter.oauthToken = null
    }

    suspend fun getRequestToken(): RequestTokenItem = twitter.fetch {
        getOAuthRequestToken("oob").toItem()
    }

    suspend fun getOauthAccessToken(
        requestToken: RequestTokenItem,
        verifier: String
    ): AccessTokenEntity = twitter.fetch {
        val token: RequestToken = (requestToken as RequestTokenItemImpl).token
        getOAuthAccessToken(token, verifier).toEntity()
    }

    private fun RequestToken.toItem(): RequestTokenItem = RequestTokenItemImpl(this)

    private fun AccessToken.toEntity(): AccessTokenEntity =
        AccessTokenEntity.create(UserId(userId), token, tokenSecret)
}

private data class RequestTokenItemImpl(override val token: RequestToken) : RequestTokenItem {
    override val authorizationUrl: String = token.authorizationURL
}
