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

package com.freshdigitable.udonroad2.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferenceDataSource @Inject constructor(
    private val prefs: SharedPreferences
) : AppSettingDataSource.Local, OAuthTokenDataSource.Local {
    override suspend fun addAccessTokenEntity(token: AccessTokenEntity) {
        val userId = token.userId
        check(userId.isValid) { "invalid token: $token" }
        val authenticatedUsers = prefs.getStringSet(AUTHENTICATED_USERS, HashSet()) ?: HashSet()
        authenticatedUsers.add("${userId.value}")

        prefs.edit {
            putStringSet(AUTHENTICATED_USERS, authenticatedUsers)
            putString("$ACCESS_TOKEN_PREFIX${userId.value}", token.token)
            putString("$TOKEN_SECRET_PREFIX${userId.value}", token.tokenSecret)
        }
    }

    fun putFetchTwitterApiConfig(timestamp: Long) {
        prefs.edit {
            putLong(TWITTER_API_CONFIG_DATE, timestamp)
        }
    }

    fun getFetchTwitterApiConfigTime(): Long {
        return prefs.getLong(TWITTER_API_CONFIG_DATE, -1)
    }

    override val currentUserId: UserId?
        get() {
            val userId = prefs.getLong(CURRENT_USER_ID, -1)
            return if (userId != -1L) UserId(userId) else null
        }

    override val currentUserIdSource: Flow<UserId> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, k ->
            if (k == CURRENT_USER_ID) {
                val id = sp.getLong(CURRENT_USER_ID, -1)
                sendBlocking(UserId(id))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart {
        currentUserId?.let { emit(it) }
    }

    override suspend fun updateCurrentUser(accessToken: AccessTokenEntity) {
        prefs.edit {
            putLong(CURRENT_USER_ID, accessToken.userId.value)
        }
    }

    override suspend fun findUserAccessTokenEntity(userId: UserId): AccessTokenEntity? {
        val token = prefs.getString("$ACCESS_TOKEN_PREFIX${userId.value}", null) ?: return null
        val secret = prefs.getString("$TOKEN_SECRET_PREFIX${userId.value}", null) ?: return null
        return AccessTokenEntity.create(userId, token, secret)
    }

    override val registeredUserIdsSource: Flow<Set<UserId>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == AUTHENTICATED_USERS) {
                val userIds = sp.getAllAuthenticatedUserIds()
                sendBlocking(userIds)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart {
        emit(prefs.getAllAuthenticatedUserIds())
    }

    fun deleteAll() {
        val users = prefs.getStringSet(AUTHENTICATED_USERS, emptySet()) ?: emptySet()
        prefs.edit {
            remove(TWITTER_API_CONFIG_DATE)
            remove(CURRENT_USER_ID)
            users.forEach { u ->
                remove("$ACCESS_TOKEN_PREFIX$u")
                remove("$TOKEN_SECRET_PREFIX$u")
            }
            remove(AUTHENTICATED_USERS)
        }
    }

    companion object {
        private const val AUTHENTICATED_USERS = "authenticatedUsers"
        private const val CURRENT_USER_ID = "currentUserId"
        private const val ACCESS_TOKEN_PREFIX = "accessToken_"
        private const val TOKEN_SECRET_PREFIX = "tokenSecret_"

        private const val TWITTER_API_CONFIG_DATE = "twitterAPIConfigDate"

        fun SharedPreferences.getAllAuthenticatedUserIds(): Set<UserId> =
            (getStringSet(AUTHENTICATED_USERS, emptySet()) ?: emptySet())
                .map { UserId(it.toLong()) }
                .toSet()
    }

    override suspend fun getRequestTokenItem(): RequestTokenItem = throw NotImplementedError()

    override suspend fun getAccessToken(
        requestToken: RequestTokenItem,
        verifier: String
    ): AccessTokenEntity = throw NotImplementedError()

    override suspend fun verifyCredentials(): UserEntity = throw NotImplementedError()
}

fun AppSettingDataSource.Local.requireCurrentUserId(): UserId = requireNotNull(currentUserId)
