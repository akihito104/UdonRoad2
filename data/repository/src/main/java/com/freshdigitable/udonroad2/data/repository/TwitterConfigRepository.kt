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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.data.restclient.TwitterConfigApiClient
import com.freshdigitable.udonroad2.model.TwitterApiConfigEntity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TwitterConfigRepository(
    private val apiClient: TwitterConfigApiClient,
    private val prefs: SharedPreferenceDataSource
) {

    private val twitterApiConfig =
        MutableLiveData<TwitterApiConfigEntity>()

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
        GlobalScope.launch {
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
