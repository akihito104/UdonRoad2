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

package com.freshdigitable.udonroad2.model

import org.threeten.bp.Instant
import java.io.Serializable

interface TweetListItem {

    val originalId: TweetId

    val originalUser: TweetingUser

    val body: Tweet

    val quoted: Tweet?

    val isRetweet: Boolean
        get() = originalId != body.id

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

interface Tweet {

    val id: TweetId

    val text: String

    val retweetCount: Int

    val favoriteCount: Int

    val user: TweetingUser

    val source: String

    val createdAt: Instant

    val mediaItems: List<MediaItem>
}

data class TweetId(val value: Long) : Serializable
