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

package com.freshdigitable.udonroad2.model.tweet

import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.TweetMediaItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import org.threeten.bp.Instant

interface TweetListItem {

    val originalId: TweetId

    val originalUser: TweetUserItem

    val body: TweetElement

    val quoted: TweetElement?

    val isRetweet: Boolean
        get() = originalId != body.id

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

interface TweetElement {

    val id: TweetId

    val text: String

    val isRetweeted: Boolean

    val retweetCount: Int

    val isFavorited: Boolean

    val favoriteCount: Int

    val inReplyToTweetId: TweetId?
        get() = null

    val retweetIdByCurrentUser: TweetId?
        get() = null

    val user: TweetUserItem

    val source: String

    val createdAt: Instant

    val media: List<TweetMediaItem>
        get() = emptyList()
}

operator fun TweetId.plus(adder: Long): TweetId = TweetId(
    this.value + adder
)

operator fun TweetId.plus(adder: Int): TweetId = TweetId(
    this.value + adder.toLong()
)

operator fun TweetId.minus(adder: Int): TweetId = TweetId(
    this.value - adder.toLong()
)

interface DetailTweetElement : TweetElement {
    val replyEntities: List<UserReplyEntity>
}

interface DetailTweetListItem : TweetListItem {
    override val body: DetailTweetElement
    override val quoted: DetailTweetElement?
}
