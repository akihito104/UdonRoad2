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
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItem
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.UserEntity
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.User

@Dao
abstract class TweetDao(
        private val db: AppDatabase
) {

    @Query("""WITH
        body AS (
        SELECT
         TweetEntityDb.id, text, created_at, retweet_count, favorite_count, source,
         UserEntity.id AS user_id,
         UserEntity.name AS user_name,
         UserEntity.screen_name AS user_screen_name,
         UserEntity.icon_url AS user_icon_url
        FROM TweetEntityDb
        INNER JOIN UserEntity ON TweetEntityDb.user_id = UserEntity.id
        ),
        original AS (
        SELECT
         TweetEntityDb.id AS original_id,
         UserEntity.id AS original_user_id,
         UserEntity.name AS original_user_name,
         UserEntity.screen_name AS original_user_screen_name,
         UserEntity.icon_url AS original_user_icon_url
        FROM TweetEntityDb
        INNER JOIN UserEntity ON TweetEntityDb.user_id = UserEntity.id
        ),
        quoted AS (
        SELECT
         TweetEntityDb.id AS qt_id,
         text AS qt_text,
         created_at AS qt_created_at,
         retweet_count AS qt_retweet_count,
         favorite_count AS qt_favorite_count,
         source AS qt_source,
         UserEntity.id AS qt_user_id,
         UserEntity.name AS qt_user_name,
         UserEntity.screen_name AS qt_user_screen_name,
         UserEntity.icon_url AS qt_user_icon_url
        FROM TweetEntityDb
        INNER JOIN UserEntity ON TweetEntityDb.user_id = UserEntity.id
        )
        SELECT body.*, original.*, quoted.*
        FROM TweetListEntity
        INNER JOIN body ON body.id = TweetListEntity.body_item_id
        INNER JOIN original ON original.original_id = TweetListEntity.original_id
        LEFT OUTER JOIN quoted ON quoted.qt_id = TweetListEntity.quoted_item_id
        WHERE TweetListEntity.owner = :owner
        ORDER BY TweetListEntity.`order` DESC""")
    abstract fun getHomeTimeline(owner: String): DataSource.Factory<Int, TweetListItem>

    @Transaction
    open fun addTweets(tweet: List<TweetEntity>) {
        val tweetEntities = tweet.asSequence()
            .map { arrayOf(it, it.retweetedTweet, it.retweetedTweet?.quotedTweet, it.quotedTweet).filterNotNull() }
            .flatMap { it.asSequence() }
            .distinctBy { it.id }
            .toList()
        val userDao = db.userDao()
        userDao.addUsers(
            tweetEntities.asSequence()
                .map { it.user }
                .filterNotNull()
                .distinctBy { it.id }
                .map { it.toDbEntity() }
                .toList())
        addTweetEntitiesInternal(tweetEntities.map(TweetEntity::toDbEntity))
        addTweetListEntities(
                tweet.map{
                    TweetListEntity(
                            originalId = it.id,
                            bodyTweetId = it.retweetedTweet?.id ?: it.id,
                            quotedTweetId = it.retweetedTweet?.quotedTweet?.id
                                    ?: it.quotedTweet?.id,
                            order = it.id, owner = "home")
                })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetEntitiesInternal(tweet: List<TweetEntityDb>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetListEntities(listEntities: List<TweetListEntity>)

    @Query("DELETE FROM TweetListEntity WHERE owner = 'home'")
    abstract fun clear()
}

@Entity(
        primaryKeys = ["original_id", "owner"],
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
            Index(
                    "original_id", "owner",
                    name = "tweet_list_entity_idx"
            ),
            Index("body_item_id")
        ]
)
class TweetListEntity(
        @ColumnInfo(name = "original_id")
        val originalId: Long,

        @ColumnInfo(name = "body_item_id")
        val bodyTweetId: Long,

        @ColumnInfo(name = "quoted_item_id")
        val quotedTweetId: Long?,

        @ColumnInfo(name = "order")
        val order: Long,

        @ColumnInfo(name = "owner")
        val owner: String
)

private fun TweetEntity.toDbEntity(): TweetEntityDb {
    return TweetEntityDb(
        id = id,
        createdAt = createdAt,
        favoriteCount = favoriteCount,
        inReplyToTweetId = inReplyToTweetId,
        isFavorited = isFavorited,
        isRetweeted = isRetweeted,
        possiblySensitive = possiblySensitive,
        quotedTweetId = quotedTweet?.id,
        retweetCount = retweetCount,
        retweetedTweetId = retweetedTweet?.id,
        source = source,
        text = text,
        userId = user.id
    )
}

private fun User.toDbEntity(): UserEntity {
    return UserEntity(
        id, name, screenName, iconUrl
    )
}
