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

package com.freshdigitable.udonroad2.tweet

import androidx.paging.DataSource
import androidx.room.*
import com.freshdigitable.udonroad2.AppDatabase

@Dao
abstract class TweetDao(
        private val db: AppDatabase
) {

    @Query("""SELECT TweetListEntity.body_item_id AS id,
         TweetEntity.text,
         TweetEntity.created_at,
         TweetEntity.retweet_count AS retweet_count,
         TweetEntity.favorite_count AS favorite_count,
         TweetEntity.source AS source,
         UserEntity.id AS user_id,
         UserEntity.name AS user_name,
         UserEntity.screen_name AS user_screen_name,
         UserEntity.icon_url AS user_icon_url,
         TweetListEntity.original_id AS original_id,
         OriginalUser.id AS original_user_id,
         OriginalUser.name AS original_user_name,
         OriginalUser.screen_name AS original_user_screen_name,
         OriginalUser.icon_url AS original_user_icon_url,
         Quoted.id AS qt_id,
         Quoted.text AS qt_text,
         Quoted.created_at AS qt_created_at,
         Quoted.retweet_count AS qt_retweet_count,
         Quoted.favorite_count AS qt_favorite_count,
         Quoted.source AS qt_source,
         QuotedUser.id AS qt_user_id,
         QuotedUser.name AS qt_user_name,
         QuotedUser.screen_name AS qt_user_screen_name,
         QuotedUser.icon_url AS qt_user_icon_url
        FROM TweetListEntity
        INNER JOIN TweetEntity ON TweetEntity.id = TweetListEntity.body_item_id
        INNER JOIN UserEntity ON TweetEntity.user_id = UserEntity.id
        INNER JOIN TweetEntity AS Original ON Original.id = TweetListEntity.original_id
        INNER JOIN UserEntity AS OriginalUser ON Original.user_id = OriginalUser.id
        LEFT OUTER JOIN TweetEntity AS Quoted ON TweetListEntity.quoted_item_id = Quoted.id
        LEFT OUTER JOIN UserEntity AS QuotedUser ON Quoted.user_id = QuotedUser.id
        WHERE TweetListEntity.owner = :owner
        ORDER BY original_id DESC""")
    abstract fun getHomeTimeline(owner: String): DataSource.Factory<Int, TweetListItem>

    @Transaction
    open fun addTweets(tweet: List<TweetEntity>) {
        val tweetEntities = tweet.asSequence()
                .map { arrayOf(it, it.retweetedTweet, it.retweetedTweet?.quotedTweet, it.quotedTweet).filterNotNull() }
                .flatMap { it.asSequence() }
//                .filterNotNull()
                .distinctBy { it.id }
                .toList()
        val userDao = db.userDao()
        userDao.addUsers(
                tweetEntities.asSequence()
                        .map { it.user }
                        .filterNotNull()
                        .distinctBy { it.id }
                        .toList())
        addTweetEntitiesInternal(tweetEntities)
        addTweetListEntities(
                tweet.map{ TweetListEntity(originalId = it.id,
                        bodyTweetId = it.retweetedTweet?.id ?: it.id,
                        quotedTweetId = it.retweetedTweet?.quotedTweet?.id ?: it.quotedTweet?.id,
                        order = it.id, owner = "home") })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetEntitiesInternal(tweet: List<TweetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetListEntities(listEntities: List<TweetListEntity>)

    @Query("DELETE FROM TweetListEntity WHERE owner = 'home'")
    abstract fun clear()
}

@Entity(
        primaryKeys = ["original_id", "owner"],
        foreignKeys = [
            ForeignKey(
                    entity = TweetEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["original_id"],
                    deferred = true
            ),
            ForeignKey(
                    entity = TweetEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["body_item_id"],
                    deferred = true
            )
        ],
        indices = [Index("original_id", "body_item_id", "owner",
                name = "tweet_list_entity_idx")]
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
