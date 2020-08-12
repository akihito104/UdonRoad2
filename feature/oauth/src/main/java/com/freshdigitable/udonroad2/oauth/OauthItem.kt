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

package com.freshdigitable.udonroad2.oauth

import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.UserId
import org.threeten.bp.Instant

data class OauthItem(
    override val originalId: TweetId,
    override val originalUser: TweetingUser,
    override val body: Tweet,
    override val quoted: Tweet? = null
) : TweetListItem

data class OauthUser(
    override val id: UserId,
    override val name: String,
    override val screenName: String,
    override val iconUrl: String
) : TweetingUser

data class OauthTweet(
    override val id: TweetId,
    override val text: String,
    override val retweetCount: Int,
    override val favoriteCount: Int,
    override val user: TweetingUser,
    override val source: String,
    override val createdAt: Instant = Instant.now(),
    override val mediaItems: List<MediaItem> = listOf(),
    override val isRetweeted: Boolean = false,
    override val isFavorited: Boolean = false
) : Tweet
