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

import com.freshdigitable.udonroad2.data.restclient.OAuthApiClient
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.user.UserId
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.Flow

class OAuthTokenRepository(
    private val apiClient: OAuthApiClient,
    private val prefs: SharedPreferenceDataSource
) {
    fun login(userId: UserId = requireNotNull(getCurrentUserId())) {
        setCurrentUserId(userId)
        val oauthAccessToken = requireNotNull(getCurrentUserAccessToken())
        apiClient.login(oauthAccessToken)
    }

    suspend fun getRequestTokenItem(): RequestTokenItem {
        apiClient.logout()
        return apiClient.getRequestToken()
    }

    suspend fun getAccessToken(
        requestToken: RequestTokenItem,
        verifier: String
    ): AccessTokenEntity {
        return apiClient.getOauthAccessToken(requestToken, verifier).also {
            storeAccessToken(it)
        }
    }

    private fun storeAccessToken(token: AccessTokenEntity) {
        prefs.storeAccessToken(token)
    }

    private fun getCurrentUserAccessToken(): AccessTokenEntity? {
        val currentUserId = prefs.getCurrentUserId() ?: return null
        if (!currentUserId.isValid) {
            return null
        }
        return prefs.getCurrentUserAccessToken()
    }

    fun getCurrentUserId(): UserId? {
        return prefs.getCurrentUserId()
    }

    fun getCurrentUserIdFlow(): Flow<UserId> = prefs.getCurrentUserIdFlow()

    private fun setCurrentUserId(userId: UserId) {
        require(prefs.isAuthenticatedUser(userId)) { "unregistered userId: $userId" }
        prefs.setCurrentUserId(userId)
    }

    fun getAllAuthenticatedUserIds(): Set<String> {
        return prefs.getAllAuthenticatedUserIds()
    }
}

@Module
interface OAuthTokenRepositoryModule {
    companion object {
        @Provides
        fun provideOAuthTokenRepository(
            apiClient: OAuthApiClient,
            prefs: SharedPreferenceDataSource
        ): OAuthTokenRepository {
            return OAuthTokenRepository(apiClient, prefs)
        }
    }
}
