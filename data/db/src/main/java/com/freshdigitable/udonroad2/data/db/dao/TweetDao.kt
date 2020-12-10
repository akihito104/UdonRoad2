/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItem
import com.freshdigitable.udonroad2.data.db.entity.MediaUrlEntity
import com.freshdigitable.udonroad2.data.db.entity.StructuredTweetEntity
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
import com.freshdigitable.udonroad2.data.db.entity.UserReplyEntityDb
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity
import com.freshdigitable.udonroad2.data.db.ext.toDbEntity
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.data.db.ext.toListEntity
import com.freshdigitable.udonroad2.data.db.ext.toStructuredTweet
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class TweetDao(
    private val db: AppDatabase
) {
    companion object {
        private const val QUERY_FIND_TWEET_LIST_ITEM_BY_ID =
            "SELECT * FROM tweet_list_item WHERE original_id = :id"
    }

    @Transaction
    @Query(
        """
        SELECT v.*
        FROM tweet_list AS l
        INNER JOIN tweet_list_item AS v ON v.original_id = l.original_id
        WHERE l.owner = :owner
        ORDER BY l.`order` DESC"""
    )
    internal abstract fun getTimeline(owner: String): DataSource.Factory<Int, TweetListItem>

    @Transaction
    @Query(QUERY_FIND_TWEET_LIST_ITEM_BY_ID)
    internal abstract fun getTweetListItemSourceById(id: TweetId): Flow<TweetListItem?>
    open fun getTweetListItemSource(
        id: TweetId
    ): Flow<com.freshdigitable.udonroad2.model.tweet.TweetListItem?> =
        getTweetListItemSourceById(id).map { it }

    @Transaction
    @Query(QUERY_FIND_TWEET_LIST_ITEM_BY_ID)
    internal abstract suspend fun findTweetListItemById(id: TweetId): TweetListItem?
    open suspend fun findTweetListItem(
        id: TweetId
    ): com.freshdigitable.udonroad2.model.tweet.TweetListItem? = findTweetListItemById(id)

    suspend fun addTweet(tweet: TweetEntity, owner: String? = null) {
        addTweets(listOf(tweet), owner)
    }

    @Query("UPDATE tweet SET is_favorited = :isFavorited WHERE id = :tweetId")
    abstract suspend fun updateFav(tweetId: TweetId, isFavorited: Boolean)

    @Query("UPDATE tweet SET is_retweeted = :isRetweeted WHERE id = :tweetId")
    abstract suspend fun updateRetweeted(tweetId: TweetId, isRetweeted: Boolean)

    @Transaction
    internal open suspend fun addTweets(
        tweet: List<TweetEntity>,
        owner: String? = null
    ) {
        val tweetEntities = tweet.asSequence()
            .map {
                arrayOf(
                    it,
                    it.retweetedTweet,
                    it.retweetedTweet?.quotedTweet,
                    it.quotedTweet
                ).filterNotNull()
            }
            .flatMap { it.asSequence() }
            .distinctBy { it.id }
            .toList()

        db.userDao().addUsers(
            tweetEntities.asSequence()
                .map { it.user }
                .filterNotNull()
                .distinctBy { it.id }
                .map { it.toEntity() }
                .toList()
        )
        addTweetEntitiesInternal(tweetEntities.map(TweetEntity::toDbEntity))
        addStructuredTweetEntities(tweet.map { it.toStructuredTweet() })
        if (owner != null) {
            addTweetListEntities(tweet.map { it.toListEntity(owner) })
        }

        val mediaItems = tweetEntities.filter { it.media.isNotEmpty() }
            .map { t -> t.media.map { t to it } }
            .flatten()
        db.mediaDao().addMediaEntities(mediaItems.map { it.second.toEntity() })
        db.mediaDao().addTweetMediaRelations(
            tweetEntities.filter { it.media.isNotEmpty() }
                .map { t ->
                    t.media.mapIndexed { i, m ->
                        MediaUrlEntity(
                            tweetId = t.id,
                            id = m.id,
                            start = m.start,
                            end = m.end,
                            order = i
                        )
                    }
                }
                .flatten()
        )
        val videoVariantEntities = mediaItems.map { it.second }
            .filter { it.videoValiantItems.isNotEmpty() }
            .flatMap { media -> media.videoValiantItems.map { media to it } }
            .map { (media, video) ->
                VideoValiantEntity(
                    mediaId = media.id,
                    url = video.url,
                    bitrate = video.bitrate,
                    contentType = video.contentType
                )
            }
        db.videoValiantDao().addVideoValiantEntities(videoVariantEntities)

        val replies = tweetEntities.map { it.id to it.replyEntities }
            .map { (id, replies) ->
                replies.map { UserReplyEntityDb(id, it.userId, it.screenName, it.start, it.end) }
            }.flatten()
        db.userReplyDao().addEntities(replies)
    }

    @Query("DELETE FROM tweet_list WHERE original_id = :id")
    abstract suspend fun deleteTweet(id: TweetId)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addTweetEntitiesInternal(tweet: List<TweetEntityDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addStructuredTweetEntities(
        listEntities: List<StructuredTweetEntity>
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addTweetListEntities(listEntities: List<TweetListEntity>)

    @Query("DELETE FROM tweet_list WHERE owner = :owner")
    abstract suspend fun clear(owner: String)
}
