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

package com.freshdigitable.udonroad2.data.local

import androidx.room.withTransaction
import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.TweetDataSource
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.db.entity.Favorited
import com.freshdigitable.udonroad2.data.db.entity.Retweeted
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetEntityUpdatable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TweetLocalDataSource @Inject constructor(
    private val db: AppDatabase,
    private val appSetting: AppSettingDataSource.Local,
) : TweetDataSource.Local {
    private val tweetDao: TweetDao = db.tweetDao()

    override suspend fun addTweetEntity(tweet: TweetEntity) {
        db.withTransaction {
            val tweets = listOf(tweet)
            tweetDao.addTweets(tweets)
            tweetDao.addReactions(tweets, appSetting.requireCurrentUserId())
        }
    }

    override fun getDetailTweetItemSource(id: TweetId): Flow<DetailTweetListItem?> =
        tweetDao.getDetailTweetListItemSourceById(id, appSetting.requireCurrentUserId())

    override suspend fun findTweetEntity(tweetId: TweetId): TweetEntity? {
        TODO("Not yet implemented")
    }

    override suspend fun findDetailTweetItem(id: TweetId): DetailTweetListItem? =
        tweetDao.findDetailTweetListItemById(id, appSetting.requireCurrentUserId())

    override suspend fun updateLike(id: TweetId, isLiked: Boolean): TweetEntityUpdatable {
        val ownerUserId = appSetting.requireCurrentUserId()
        when (isLiked) {
            true -> db.reactionsDao.addFav(Favorited(id, ownerUserId))
            false -> db.reactionsDao.deleteFav(id, ownerUserId)
        }
        return checkNotNull(tweetDao.findTweetEntityUpdatable(id, ownerUserId))
    }

    override suspend fun updateRetweet(id: TweetId, isRetweeted: Boolean): TweetEntityUpdatable {
        val ownerUserId = appSetting.requireCurrentUserId()
        when (isRetweeted) {
            true -> {
                db.reactionsDao.addRetweeted(
                    Retweeted(tweetId = id, sourceUserId = ownerUserId, null)
                )
            }
            false -> db.reactionsDao.deleteRetweeted(id, ownerUserId)
        }
        return checkNotNull(tweetDao.findTweetEntityUpdatable(id, ownerUserId))
    }

    override suspend fun updateTweet(tweet: TweetEntityUpdatable) {
        val currentUserId = appSetting.requireCurrentUserId()
        db.withTransaction {
            listOfNotNull(
                tweet,
                tweet.retweetedTweet,
                tweet.retweetedTweet?.quotedTweet,
                tweet.quotedTweet
            ).forEach { element ->
                tweetDao.updateTweetElement(
                    id = element.id,
                    favCount = element.favoriteCount,
                    retweetCount = element.retweetCount
                )
                tweetDao.addReactions(listOf(element), currentUserId)
                // TODO: update UserEntity
            }
        }
    }

    override suspend fun deleteTweet(id: TweetId) {
        tweetDao.deleteTweet(id)
    }
}
