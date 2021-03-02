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

package com.freshdigitable.udonroad2.test_common.jvm

import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import io.mockk.every
import io.mockk.mockk

fun TweetEntity.Companion.createMock(
    id: Int,
    isRetweeted: Boolean = false,
    isFavorited: Boolean = false,
    replyTo: TweetId? = null,
    retweeted: TweetEntity? = null,
    quoted: TweetEntity? = null,
    replyEntity: List<UserReplyEntity> = emptyList(),
    media: List<MediaEntity> = emptyList(),
): TweetEntity = createMock(
    TweetId(id.toLong()),
    isRetweeted,
    isFavorited,
    replyTo,
    retweeted,
    quoted,
    replyEntity,
    media
)

fun TweetEntity.Companion.createMock(
    id: TweetId,
    isRetweeted: Boolean = false,
    isFavorited: Boolean = false,
    replyTo: TweetId? = null,
    retweeted: TweetEntity? = null,
    quoted: TweetEntity? = null,
    replyEntity: List<UserReplyEntity> = emptyList(),
    media: List<MediaEntity> = emptyList(),
): TweetEntity = mockk<TweetEntity>(relaxed = true).also {
    every { it.id } returns id
    every { it.isFavorited } returns isFavorited
    every { it.isRetweeted } returns isRetweeted
    every { it.retweetedTweet } returns retweeted
    every { it.retweetIdByCurrentUser } returns null
    every { it.quotedTweet } returns quoted
    every { it.replyEntities } returns replyEntity
    every { it.media } returns media
    every { it.inReplyToTweetId } returns replyTo
}

fun TweetListItem.Companion.createMock(
    originalTweetId: TweetId,
    body: TweetElement? = null
): TweetListItem = mockk<TweetListItem>().also {
    every { it.originalId } returns originalTweetId

    val b = body ?: mockk<TweetElement>().also {
        every { it.id } returns originalTweetId
    }
    every { it.body } returns b
}

fun TweetElement.Companion.createMock(
    id: TweetId,
    media: List<MediaEntity> = emptyList()
): TweetElement = mockk<TweetElement>().also {
    every { it.id } returns id
    every { it.media } returns media
}
