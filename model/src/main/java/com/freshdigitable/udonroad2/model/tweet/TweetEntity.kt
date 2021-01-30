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

import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity

interface TweetEntity : TweetElement, TweetEntityUpdatable {

    override val user: UserEntity

    override val retweetedTweet: TweetEntity?

    override val quotedTweet: TweetEntity?

    val possiblySensitive: Boolean

    val replyEntities: List<UserReplyEntity>

    override val media: List<MediaEntity>

    companion object
}

interface TweetEntityUpdatable : TweetElementUpdatable {
    val retweetedTweet: TweetEntityUpdatable?
    val quotedTweet: TweetEntityUpdatable?
}

interface UserReplyEntity {
    val userId: UserId
    val screenName: String
    val start: Int
    val end: Int

    companion object
}
