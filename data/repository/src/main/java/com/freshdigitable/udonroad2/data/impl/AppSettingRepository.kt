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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.data.UserRepository
import com.freshdigitable.udonroad2.data.local.SharedPreferenceDataSource
import com.freshdigitable.udonroad2.data.restclient.OAuthApiClient
import com.freshdigitable.udonroad2.data.restclient.TwitterConfigApiClient
import com.freshdigitable.udonroad2.model.TwitterApiConfigEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingRepository @Inject constructor(
    private val apiClient: TwitterConfigApiClient,
    private val oAuthApiClient: OAuthApiClient,
    private val prefs: SharedPreferenceDataSource,
    private val userDao: UserRepository.LocalSource,
    private val executor: AppExecutor
) {
    val currentUserId: UserId?
        get() = prefs.getCurrentUserId()
    val currentUserIdSource: Flow<UserId> = prefs.getCurrentUserIdFlow()

    suspend fun updateCurrentUser(userId: UserId) {
        if (!prefs.isAuthenticatedUser(userId)) {
            throw IllegalArgumentException("userId: ${userId.value} is not registered...")
        }
        prefs.setCurrentUserId(userId)
        val token = requireNotNull(prefs.findUserAccessToken(userId))
        val user = oAuthApiClient.login(token)
        userDao.addUser(user)
    }

    fun getAllAuthenticatedUserIds(): Set<String> = prefs.getAllAuthenticatedUserIds()

    private val twitterApiConfig = MutableLiveData<TwitterApiConfigEntity>()

    fun getTwitterAPIConfig(): LiveData<TwitterApiConfigEntity> {
        if (isTwitterAPIConfigFetchable() || twitterApiConfig.value == null) {
            fetchTwitterAPIConfig()
        }
        return twitterApiConfig
    }

    private fun setFetchTwitterAPIConfigTime(timestamp: Long) {
        prefs.putFetchTwitterApiConfig(timestamp)
    }

    private fun getFetchTwitterAPIConfigTime(): Long {
        return prefs.getFetchTwitterApiConfigTime()
    }

    private fun isTwitterAPIConfigFetchable(): Boolean {
        val lastTime = getFetchTwitterAPIConfigTime()
        if (lastTime == -1L) {
            return true
        }
        val now = System.currentTimeMillis()
        return now - lastTime > TimeUnit.DAYS.toMillis(1)
    }

    private fun fetchTwitterAPIConfig() {
        executor.launchIO {
            val conf = apiClient.getTwitterApiConfig()
            setFetchTwitterAPIConfigTime(System.currentTimeMillis())
//        realm.executeTransaction({ r ->
//            r.delete(TwitterAPIConfigurationRealm::class.java)
//            val twitterAPIConfiguration = TwitterAPIConfigurationRealm(twitterAPIConfig)
//            r.insertOrUpdate(twitterAPIConfiguration)
//        })
            twitterApiConfig.postValue(conf)
        }
    }
}
