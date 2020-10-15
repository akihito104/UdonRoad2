/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@ActivityScope
class TweetInputRepository @Inject constructor(
    private val remoteSource: TweetApiClient
) {
    private val _text: MutableStateFlow<String> = MutableStateFlow("")
    val text: Flow<String> = _text

    fun updateText(text: String = "") {
        _text.value = text
    }

    fun post(): Flow<TweetEntity> {
        val t = _text.value
        return flow {
            emit(remoteSource.postTweet(t))
        }
    }
}
