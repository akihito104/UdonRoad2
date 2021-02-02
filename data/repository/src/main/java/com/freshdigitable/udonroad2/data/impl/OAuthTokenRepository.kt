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

package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.user.UserEntity

internal class OAuthTokenRepository(
    private val prefs: OAuthTokenDataSource.Local,
    private val apiClient: OAuthTokenDataSource.Remote,
) : OAuthTokenDataSource by prefs {
    override suspend fun getRequestTokenItem(): RequestTokenItem = apiClient.getRequestTokenItem()

    override suspend fun getAccessToken(
        requestToken: RequestTokenItem,
        verifier: String
    ): AccessTokenEntity {
        val accessToken = apiClient.getAccessToken(requestToken, verifier)
        prefs.addAccessTokenEntity(accessToken)
        return accessToken
    }

    override suspend fun verifyCredentials(): UserEntity = apiClient.verifyCredentials()
}
