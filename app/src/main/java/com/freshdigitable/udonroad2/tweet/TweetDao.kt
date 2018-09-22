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

    @Query("SELECT TweetEntity.id AS id," +
            " TweetEntity.text, " +
            " TweetEntity.retweet_count AS retweet_count, " +
            " TweetEntity.favorite_count AS favorite_count, " +
            " User.id AS user_id, " +
            " User.name AS user_name, " +
            " User.screen_name AS user_screen_name, " +
            " User.icon_url AS user_icon_url " +
            "FROM TweetEntity " +
            "INNER JOIN User ON TweetEntity.user_id = User.id " +
            "ORDER BY id DESC")
    abstract fun getHomeTimeline(): DataSource.Factory<Int, Tweet>

    @Transaction
    open fun addTweets(tweet: List<TweetEntity>) {
        val userDao = db.userDao()
        userDao.addUsers(
                tweet.asSequence()
                        .map { it.user }
                        .filterNotNull()
                        .distinctBy { it.id }
                        .toList())
        addTweetEntitiesInternal(tweet)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetEntitiesInternal(tweet: List<TweetEntity>)

    @Query("DELETE FROM TweetEntity")
    abstract fun clear()
}
