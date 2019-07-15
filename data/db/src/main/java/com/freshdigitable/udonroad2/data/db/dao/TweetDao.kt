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

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dbview.Tweet
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItem
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetMediaRelation
import com.freshdigitable.udonroad2.data.db.ext.toDbEntity
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.data.db.ext.toListEntity
import com.freshdigitable.udonroad2.data.db.ext.toStructuredTweet
import com.freshdigitable.udonroad2.model.TweetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
abstract class TweetDao(
    private val db: AppDatabase
) {

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
    @Query("SELECT * FROM tweet_list_item WHERE original_id = :id")
    internal abstract fun findTweetItem(id: Long): LiveData<TweetListItem?>

    @Transaction
    @Query("SELECT * FROM view_tweet WHERE id = :id")
    internal abstract fun findTweet(id: Long): LiveData<Tweet?>

    open fun findTweetItemById(
        id: Long
    ): LiveData<com.freshdigitable.udonroad2.model.TweetListItem?> = findTweetItem(id).map { it }

    suspend fun addTweet(tweet: TweetEntity, owner: String? = null) = withContext(Dispatchers.IO) {
        addTweets(listOf(tweet), owner)
    }

    @Transaction
    internal open suspend fun addTweets(
        tweet: List<TweetEntity>,
        owner: String? = null
    ) = withContext(Dispatchers.IO) {
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
                .toList())
        addTweetEntitiesInternal(tweetEntities.map(TweetEntity::toDbEntity))
        addStructuredTweetEntities(tweet.map { it.toStructuredTweet() })
        if (owner != null) {
            addTweetListEntities(tweet.map { it.toListEntity(owner) })
        }

        val mediaItems = tweetEntities.filter { it.media.isNotEmpty() }
            .map { t -> t.media.map { t to it } }
            .flatten()
        db.urlDao().addUrlEntities(mediaItems.map { it.second.url.toEntity() })
        db.mediaDao().addMediaEntities(mediaItems.map { it.second.toEntity() })
        db.mediaDao().addTweetMediaRelations(mediaItems.map {
            TweetMediaRelation(
                it.first.id,
                it.second.id
            )
        })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addTweetEntitiesInternal(tweet: List<TweetEntityDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addStructuredTweetEntities(listEntities: List<StructuredTweetEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addTweetListEntities(listEntities: List<TweetListEntity>)

    @Query("DELETE FROM tweet_list WHERE owner = :owner")
    abstract suspend fun clear(owner: String)
}

@Entity(
    tableName = "structured_tweet",
    foreignKeys = [
        ForeignKey(
            entity = TweetEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["original_id"],
            deferred = true
        ),
        ForeignKey(
            entity = TweetEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["body_item_id"],
            deferred = true
        )
    ],
    indices = [
        Index("body_item_id")
    ]
)
internal class StructuredTweetEntity(
    @PrimaryKey
    @ColumnInfo(name = "original_id")
    val originalId: Long,

    @ColumnInfo(name = "body_item_id")
    val bodyTweetId: Long,

    @ColumnInfo(name = "quoted_item_id")
    val quotedTweetId: Long?
)

@Entity(
    tableName = "tweet_list",
    primaryKeys = ["original_id", "owner"],
    foreignKeys = [
        ForeignKey(
            entity = StructuredTweetEntity::class,
            parentColumns = ["original_id"],
            childColumns = ["original_id"],
            deferred = true
        )
    ],
    indices = [
        Index(
            "original_id", "owner",
            name = "tweet_list_entity_idx"
        )
    ]
)
internal class TweetListEntity(
    @ColumnInfo(name = "original_id")
    val originalId: Long,

    @ColumnInfo(name = "order")
    val order: Long,

    @ColumnInfo(name = "owner")
    val owner: String
)
