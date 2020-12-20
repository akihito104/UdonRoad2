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
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class TweetDao(
    private val db: AppDatabase
) {
    companion object {
        private const val QUERY_TWEET_LIST_ITEM_MEMBERS = """
        SELECT v.*, 
         b_r.tweet_id NOT NULL AS is_retweeted, 
         b_r.retweet_id AS retweet_id_by_current_user,
         b_f.tweet_id NOT NULL AS is_favorited,
         q_r.tweet_id NOT NULL AS qt_is_retweeted, 
         q_r.retweet_id AS qt_retweet_id_by_current_user,
         q_f.tweet_id NOT NULL AS qt_is_favorited
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
           AND v.id = q_f.tweet_id"""
        private const val QUERY_FIND_TWEET_LIST_ITEM_BY_ID =
            "$QUERY_TWEET_LIST_ITEM_MEMBERS WHERE l.original_id = :id LIMIT 1"
    }

    @Transaction
    @Query("$QUERY_TWEET_LIST_ITEM_MEMBERS WHERE l.list_id = :owner ORDER BY l.original_id DESC")
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

    @Transaction
    open suspend fun addTweet(tweet: TweetEntity, ownerUserId: UserId) {
        val tweets = listOf(tweet)
        addTweets(tweets)
        addReactions(tweets, ownerUserId)
    }

    suspend fun updateFav(tweetId: TweetId, ownerUserId: UserId, isFavorited: Boolean) {
        when (isFavorited) {
            true -> db.reactionsDao.addFav(Favorited(tweetId, ownerUserId))
            false -> db.reactionsDao.deleteFav(tweetId, ownerUserId)
        }
    }

    suspend fun updateRetweeted(
        tweetId: TweetId,
        retweetId: TweetId?,
        ownerUserId: UserId,
        isRetweeted: Boolean
    ) {
        when (isRetweeted) {
            true -> {
                db.reactionsDao.addRetweeted(
                    Retweeted(tweetId = tweetId, sourceUserId = ownerUserId, retweetId = retweetId)
                )
            }
            false -> db.reactionsDao.deleteRetweeted(tweetId, ownerUserId)
        }
    }

    suspend fun findRetweetIdByTweetId(tweetId: TweetId, currentUserId: UserId): TweetId? {
        return db.reactionsDao.findRetweetId(tweetId, currentUserId)
    }

    @Transaction
    internal open suspend fun addTweetsToList(
        tweet: List<TweetEntity>,
        owner: ListId
    ) {
        addTweets(tweet)
        addTweetListEntities(tweet.map { it.toListEntity(owner) })

        val listOwner = db.listDao().getListById(owner)
        addReactions(tweet, listOwner.ownerId)
    }

    private suspend fun addTweets(tweet: List<TweetEntity>) {
        val tweetEntities = tweet.asSequence()
            .flatMap {
                listOfNotNull(
                    it,
                    it.retweetedTweet,
                    it.retweetedTweet?.quotedTweet,
                    it.quotedTweet
                )
            }
            .distinctBy { it.id }
            .toList()

        db.userDao().addUsers(
            tweetEntities.asSequence()
                .mapNotNull { it.user }
                .distinctBy { it.id }
                .map { it.toEntity() }
                .toList()
        )
        addTweetEntitiesInternal(tweetEntities.map(TweetEntity::toDbEntity))
        addTweetEntities(tweet.map { it.toTweetEntityDb() })

        val mediaItems = tweetEntities.filter { it.media.isNotEmpty() }
            .flatMap { t -> t.media.map { t to it } }
        db.mediaDao().addMediaEntities(mediaItems.map { it.second.toEntity() })
        db.mediaDao().addTweetMediaRelations(
            tweetEntities.filter { it.media.isNotEmpty() }
                .flatMap { t ->
                    t.media.mapIndexed { i, m -> MediaUrlEntity.create(t.id, m, i) }
                }
        )
        db.videoValiantDao().addVideoValiantEntities(
            mediaItems.map { it.second }
                .filter { it.videoValiantItems.isNotEmpty() }
                .flatMap { media ->
                    media.videoValiantItems.map { VideoValiantEntity.create(media.id, it) }
                }
        )

        db.userReplyDao().addEntities(
            tweetEntities.map { it.id to it.replyEntities }
                .flatMap { (id, replies) ->
                    replies.map { UserReplyEntityDb.create(id, it) }
                }
        )
    }

    private suspend fun addReactions(tweet: List<TweetEntity>, ownerUserId: UserId) {
        val tweetsMayHaveReactions = tweet.flatMap { listOfNotNull(it, it.retweetedTweet) }
            .distinctBy { it.id }
        db.reactionsDao.addRetweeteds(
            tweetsMayHaveReactions
                .filter { it.isRetweeted && it.retweetedTweet == null }
                .map { Retweeted(it.id, ownerUserId, it.retweetIdByCurrentUser) }
        )
        db.reactionsDao.addFavs(
            tweetsMayHaveReactions
                .filter { it.isFavorited }
                .map { Favorited(it.id, ownerUserId) }
        )
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

@Dao
internal interface ReactionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRetweeted(retweeted: Retweeted)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRetweeteds(retweeteds: List<Retweeted>)

    @Query("DELETE FROM retweeted WHERE tweet_id = :tweetId AND source_user_id = :sourceUserId")
    suspend fun deleteRetweeted(tweetId: TweetId, sourceUserId: UserId)

    @Query("SELECT retweet_id FROM retweeted WHERE source_user_id = :currentUserId AND (tweet_id = :tweetId OR retweet_id = :tweetId)")
    suspend fun findRetweetId(tweetId: TweetId, currentUserId: UserId): TweetId?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFav(favorited: Favorited)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavs(favorited: List<Favorited>)

    @Query("DELETE FROM favorited WHERE tweet_id = :tweetId AND source_user_id = :userId")
    suspend fun deleteFav(tweetId: TweetId, userId: UserId)
}

private fun MediaUrlEntity.Companion.create(
    tweetId: TweetId,
    media: MediaEntity,
    order: Int
): MediaUrlEntity = MediaUrlEntity(
    tweetId = tweetId,
    id = media.id,
    start = media.start,
    end = media.end,
    order = order
)

private fun VideoValiantEntity.Companion.create(
    mediaId: MediaId,
    video: MediaEntity.VideoValiant
): VideoValiantEntity = VideoValiantEntity(
    mediaId = mediaId,
    url = video.url,
    bitrate = video.bitrate,
    contentType = video.contentType
)

private fun UserReplyEntityDb.Companion.create(id: TweetId, reply: UserReplyEntity) =
    UserReplyEntityDb(id, reply.userId, reply.screenName, reply.start, reply.end)
