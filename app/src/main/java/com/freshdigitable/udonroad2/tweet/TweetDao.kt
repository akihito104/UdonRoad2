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

    @Query("SELECT TweetListEntity.item_id AS id," +
            " TweetEntity.text, " +
            " TweetEntity.retweet_count AS retweet_count, " +
            " TweetEntity.favorite_count AS favorite_count, " +
            " TweetEntity.source AS source, " +
            " UserEntity.id AS user_id, " +
            " UserEntity.name AS user_name, " +
            " UserEntity.screen_name AS user_screen_name, " +
            " UserEntity.icon_url AS user_icon_url " +
            "FROM TweetListEntity " +
            "INNER JOIN TweetEntity ON TweetEntity.id = TweetListEntity.item_id " +
            "INNER JOIN UserEntity ON TweetEntity.user_id = UserEntity.id " +
            "WHERE TweetListEntity.owner = 'home' "+
            "ORDER BY id DESC")
    abstract fun getHomeTimeline(): DataSource.Factory<Int, Tweet>

    @Transaction
    open fun addTweets(tweet: List<TweetEntity>) {
        addTweetListEntities(
                tweet.map{ TweetListEntity(itemId = it.id, order = it.id, owner = "home") })
        val userDao = db.userDao()
        val tweetEntities = tweet.asSequence()
                .map { arrayOf(it, it.retweetedTweet, it.retweetedTweet?.quotedTweet, it.quotedTweet) }
                .flatMap { it.asSequence() }
                .filterNotNull()
                .distinctBy { it.id }
                .toList()
        addTweetEntitiesInternal(tweetEntities)
        userDao.addUsers(
                tweetEntities.asSequence()
                        .map { it.user }
                        .filterNotNull()
                        .distinctBy { it.id }
                        .toList())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetEntitiesInternal(tweet: List<TweetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetListEntities(listEntities: List<TweetListEntity>)

    @Query("DELETE FROM TweetListEntity WHERE owner = 'home'")
    abstract fun clear()
}

@Entity(
        primaryKeys = ["item_id", "owner"],
        foreignKeys = [
            ForeignKey(
                    entity = TweetEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["item_id"],
                    deferred = true
            )
        ],
        indices = [Index("item_id", "owner")]
)
class TweetListEntity(
        @ColumnInfo(name = "item_id")
        val itemId: Long,

        @ColumnInfo(name = "order")
        val order: Long,

        @ColumnInfo(name = "owner")
        val owner: String
)
