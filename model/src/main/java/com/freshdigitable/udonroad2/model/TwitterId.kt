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

package com.freshdigitable.udonroad2.model

import java.io.Serializable

interface TwitterId : Serializable {
    /**
     * twitter id (for tweet, direct massage, user, list...) is 64 bit unsigned integer.
     * kotlin ULong is now experimental status.
     */
    val value: Long
}

data class TweetId(override val value: Long) : TwitterId

data class MediaId(override val value: Long) : TwitterId

data class CustomTimelineId(override val value: Long) : TwitterId {
    companion object {
        fun create(value: Long?): CustomTimelineId? = value?.let { CustomTimelineId(it) }
    }
}

data class UserId(override val value: Long) : TwitterId {
    val isValid: Boolean = value >= 0

    companion object {
        fun create(value: Long?): UserId? = value?.let { UserId(it) }
    }
}
