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
import com.freshdigitable.udonroad2.model.UrlItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import org.threeten.bp.Instant

interface TweetListItem {

    val originalId: TweetId

    val originalUser: TweetUserItem

    val body: TweetElement

    val quoted: TweetElement?

    val isRetweet: Boolean
        get() = originalId != body.id

    val bodyTextWithDisplayUrl: String
        get() = when (val qId = quoted?.id) {
            null -> body.textWithDisplayUrl
            else -> {
                body.urlItems.fold(body.text) { t, url ->
                    val displayUrl =
                        if (url.expandedUrl.contains(qId.value.toString())) "" else url.displayUrl
                    t.replace(url.url, displayUrl)
                }.also {
                    body.media.fold(it) { t, url -> t.replace(url.mediaUrl, url.displayUrl) }
                }
            }
        }

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    companion object {
        val TweetListItem.permalink: String
            get() = "https://twitter.com/${originalUser.screenName}/status/${originalId.value}"
    }
}

interface TweetElement : TweetElementUpdatable {

    val text: String

    val inReplyToTweetId: TweetId?
        get() = null

    val source: String

    val createdAt: Instant

    val urlItems: List<UrlItem>

    val media: List<TweetMediaItem>
        get() = emptyList()

    val possiblySensitive: Boolean

    val textWithDisplayUrl: String
        get() = (urlItems + media).fold(text) { t, url -> t.replace(url.url, url.displayUrl) }

    companion object
}

interface TweetElementUpdatable {
    val id: TweetId
    val isRetweeted: Boolean
    val retweetCount: Int
    val isFavorited: Boolean
    val favoriteCount: Int
    val retweetIdByCurrentUser: TweetId?
        get() = null
    val user: TweetUserItem
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

    companion object
}
