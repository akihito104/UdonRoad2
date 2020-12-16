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
import com.freshdigitable.udonroad2.data.db.entity.Favorited
import com.freshdigitable.udonroad2.data.db.entity.MediaUrlEntity
import com.freshdigitable.udonroad2.data.db.entity.Retweeted
import com.freshdigitable.udonroad2.data.db.entity.TweetElementDb
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
import com.freshdigitable.udonroad2.data.db.entity.UserReplyEntityDb
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity
import com.freshdigitable.udonroad2.data.db.ext.toDbEntity
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.data.db.ext.toListEntity
import com.freshdigitable.udonroad2.data.db.ext.toTweetEntityDb
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class TweetDao(
    private val db: AppDatabase
) {
    companion object {
        private const val QUERY_FIND_TWEET_LIST_ITEM_BY_ID =
            """SELECT v.*, b_r.tweet_id NOT NULL AS is_retweeted, b_f.tweet_id NOT NULL AS is_favorited,
         q_r.tweet_id NOT NULL AS qt_is_retweeted, q_f.tweet_id NOT NULL AS qt_is_favorited
        FROM tweet_list AS l
        INNER JOIN list ON l.list_id = list.id
        INNER JOIN view_tweet_list_item AS v ON v.original_id = l.original_id
        LEFT OUTER JOIN retweeted AS b_r ON list.owner_id = b_r.source_user_id
          AND v.id = b_r.tweet_id
        LEFT OUTER JOIN favorited AS b_f ON list.owner_id = b_f.source_user_id
          AND v.id = b_f.tweet_id
        LEFT OUTER JOIN retweeted AS q_r ON list.owner_id = q_r.source_user_id
          AND v.id = q_r.tweet_id
        LEFT OUTER JOIN favorited AS q_f ON list.owner_id = q_f.source_user_id
          AND v.id = q_f.tweet_id
        WHERE l.original_id = :id LIMIT 1"""
    }

    @Transaction
    @Query(
        """
        SELECT v.*, b_r.tweet_id NOT NULL AS is_retweeted, b_f.tweet_id NOT NULL AS is_favorited,
         q_r.tweet_id NOT NULL AS qt_is_retweeted, q_f.tweet_id NOT NULL AS qt_is_favorited
        FROM tweet_list AS l
        INNER JOIN list ON l.list_id = list.id
        INNER JOIN view_tweet_list_item AS v ON v.original_id = l.original_id
        LEFT OUTER JOIN retweeted AS b_r ON list.owner_id = b_r.source_user_id
          AND v.id = b_r.tweet_id
        LEFT OUTER JOIN favorited AS b_f ON list.owner_id = b_f.source_user_id
          AND v.id = b_f.tweet_id
        LEFT OUTER JOIN retweeted AS q_r ON list.owner_id = q_r.source_user_id
          AND v.id = q_r.tweet_id
        LEFT OUTER JOIN favorited AS q_f ON list.owner_id = q_f.source_user_id
          AND v.id = q_f.tweet_id
        WHERE l.list_id = :owner
        ORDER BY l.original_id DESC"""
    )
    internal abstract fun getTimeline(owner: ListId): DataSource.Factory<Int, TweetListItem>

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

    suspend fun addTweet(tweet: TweetEntity, owner: ListId? = null) {
        addTweets(listOf(tweet), owner)
    }

    suspend fun updateFav(tweetId: TweetId, isFavorited: Boolean) {
        when (isFavorited) {
            true -> addFav(Favorited(tweetId, UserId(0))) // FIXME
            false -> deleteFav(tweetId, UserId(0)) // FIXME
        }
    }

    @Insert
    internal abstract suspend fun addFav(favorited: Favorited)

    @Query("DELETE FROM favorited WHERE tweet_id = :tweetId AND source_user_id = :userId")
    internal abstract suspend fun deleteFav(tweetId: TweetId, userId: UserId)

    suspend fun updateRetweeted(tweetId: TweetId, retweetedId: TweetId?, isRetweeted: Boolean) {
        when (isRetweeted) {
            true -> addRetweeted(
                Retweeted(
                    tweetId = tweetId,
                    sourceUserId = UserId(0), // FIXME
                    retweetId = retweetedId!!
                )
            )
            false -> deleteRetweeted(tweetId, UserId(0)) // FIXME
        }
    }

    @Insert
    internal abstract suspend fun addRetweeted(retweeted: Retweeted)

    @Query("DELETE FROM retweeted WHERE tweet_id = :tweetId AND source_user_id = :sourceUserId")
    internal abstract suspend fun deleteRetweeted(tweetId: TweetId, sourceUserId: UserId)

    @Transaction
    internal open suspend fun addTweets(
        tweet: List<TweetEntity>,
        owner: ListId? = null
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
        addTweetEntities(tweet.map { it.toTweetEntityDb() })
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
    internal abstract suspend fun addTweetEntitiesInternal(tweet: List<TweetElementDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addTweetEntities(listEntities: List<TweetEntityDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addTweetListEntities(listEntities: List<TweetListEntity>)

    @Query("DELETE FROM tweet_list WHERE list_id = :owner")
    internal abstract suspend fun deleteByListId(owner: ListId)

    suspend fun clear(owner: ListId) {
        db.listDao().deleteList(owner)
    }
}
