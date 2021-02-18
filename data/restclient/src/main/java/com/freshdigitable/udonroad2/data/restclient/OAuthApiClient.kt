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

import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OAuthApiClient @Inject constructor(
    private val twitter: AppTwitter
) : OAuthTokenDataSource.Remote, AppSettingDataSource.Remote {

    override val currentUserId: UserId?
        get() = twitter.oauthToken?.userId

    override suspend fun updateCurrentUser(accessToken: AccessTokenEntity) {
        twitter.oauthToken = accessToken
    }

    override suspend fun verifyCredentials(): UserEntity =
        twitter.fetch { verifyCredentials().toEntity() }

    override suspend fun getRequestTokenItem(): RequestTokenItem = twitter.run {
        oauthToken = null
        return fetch { getOAuthRequestToken("oob").toItem() }
    }

    override suspend fun getAccessToken(
        requestToken: RequestTokenItem,
        verifier: String
    ): AccessTokenEntity = twitter.fetch {
        val token: RequestToken = (requestToken as RequestTokenItemImpl).token
        getOAuthAccessToken(token, verifier).toEntity()
    }

    private fun RequestToken.toItem(): RequestTokenItem = RequestTokenItemImpl(this)

    private fun AccessToken.toEntity(): AccessTokenEntity =
        AccessTokenEntity.create(UserId(userId), token, tokenSecret)

    override val currentUserIdSource: Flow<UserId>
        get() = throw NotImplementedError()
    override val registeredUserIdsSource: Flow<Set<UserId>>
        get() = throw NotImplementedError()
    override val loginAccountOnLaunch: UserId
        get() = throw NotImplementedError()
    override val isPossiblySensitiveHidden: Flow<Boolean>
        get() = throw NotImplementedError()

    override suspend fun addAccessTokenEntity(token: AccessTokenEntity) =
        throw NotImplementedError()

    override suspend fun findUserAccessTokenEntity(userId: UserId): AccessTokenEntity =
        throw NotImplementedError()
}

private data class RequestTokenItemImpl(override val token: RequestToken) : RequestTokenItem {
    override val authorizationUrl: String = token.authorizationURL
}
