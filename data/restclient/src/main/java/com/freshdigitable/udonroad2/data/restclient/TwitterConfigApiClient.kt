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

import com.freshdigitable.udonroad2.model.TwitterApiConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.TwitterAPIConfiguration
import javax.inject.Inject

class TwitterConfigApiClient @Inject constructor(
    private val twitter: Twitter
) {
    suspend fun getTwitterApiConfig(): TwitterApiConfigEntity =
        withContext(Dispatchers.IO) {
            twitter.apiConfiguration.toEntity()
        }

    private fun TwitterAPIConfiguration.toEntity(): TwitterApiConfigEntity {
        return TwitterApiConfigEntity(
            photoSizeLimit = this.photoSizeLimit,
            shortUrlLength = this.shortURLLength,
            shortUrlLengthHttps = this.shortURLLengthHttps,
            charactersReservedPerMedia = this.charactersReservedPerMedia,
            dmTextCharacterLimit = this.dmTextCharacterLimit,
            maxMediaPerUpload = this.maxMediaPerUpload
        )
    }
}
