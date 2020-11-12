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
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import java.io.InputStream
import javax.inject.Inject

class TweetInputRepository @Inject constructor(
    private val remoteSource: TweetApiClient
) {
    suspend fun post(text: String, mediaIds: List<MediaId>): TweetEntity {
        return remoteSource.postTweet(text, mediaIds)
    }

    suspend fun uploadMedia(filename: String, inputStream: InputStream): MediaId {
        return remoteSource.uploadMedia(filename, inputStream)
    }
}
