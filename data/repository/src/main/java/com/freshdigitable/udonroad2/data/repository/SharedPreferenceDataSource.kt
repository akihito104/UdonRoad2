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

package com.freshdigitable.udonroad2.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferenceDataSource @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun storeAccessToken(token: AccessTokenEntity) {
        val userId = token.userId
        if (userId < 0) {
            return
        }
        val authenticatedUsers = prefs.getStringSet(AUTHENTICATED_USERS, HashSet()) ?: HashSet()
        authenticatedUsers.add("$userId")

        prefs.edit {
            putStringSet(AUTHENTICATED_USERS, authenticatedUsers)
            putString("$ACCESS_TOKEN_PREFIX$userId", token.token)
            putString("$TOKEN_SECRET_PREFIX$userId", token.tokenSecret)
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

    fun getCurrentUserId(): Long? {
        val userId = prefs.getLong(CURRENT_USER_ID, -1)
        return if (userId != -1L) userId else null
    }

    fun getCurrentUserAccessToken(): AccessTokenEntity? {
        val currentUserId = getCurrentUserId() ?: return null
        val token = prefs.getString("$ACCESS_TOKEN_PREFIX$currentUserId", null)
        val secret = prefs.getString("$TOKEN_SECRET_PREFIX$currentUserId", null)
        return if (token == null || secret == null) {
            null
        } else {
            AccessTokenEntity.create(
                currentUserId,
                token,
                secret
            )
        }
    }

    fun isAuthenticatedUser(userId: Long): Boolean {
        val userIds = prefs.getStringSet(AUTHENTICATED_USERS, emptySet()) ?: return false
        return userIds.any { it == "$userId" }
    }

    fun setCurrentUserId(userId: Long) {
        prefs.edit {
            putLong(CURRENT_USER_ID, userId)
        }
    }

    fun getAllAuthenticatedUserIds(): Set<String> {
        return prefs.getStringSet(AUTHENTICATED_USERS, emptySet()) ?: emptySet()
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
    }
}
