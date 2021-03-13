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

package com.freshdigitable.udonroad2.data

import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.TwitterCard
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetEntityUpdatable
import kotlinx.coroutines.flow.Flow

interface TweetDataSource {
    suspend fun addTweetEntity(tweet: TweetEntity)

    fun getDetailTweetItemSource(id: TweetId): Flow<DetailTweetListItem?>
    suspend fun findTweetEntity(tweetId: TweetId): TweetEntity?
    suspend fun findDetailTweetItem(id: TweetId): DetailTweetListItem?

    suspend fun updateLike(id: TweetId, isLiked: Boolean): TweetEntityUpdatable
    suspend fun updateRetweet(id: TweetId, isRetweeted: Boolean): TweetEntityUpdatable
    suspend fun updateTweet(tweet: TweetEntityUpdatable)

    suspend fun deleteTweet(id: TweetId)

    interface Local : TweetDataSource
    interface Remote : TweetDataSource
}

interface TwitterCardDataSource {
    fun getTwitterCardSource(url: String): Flow<TwitterCard>

    interface Local : TwitterCardDataSource
    interface Remote : TwitterCardDataSource
}
