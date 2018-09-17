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
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freshdigitable.udonroad2.AppDatabase
import com.freshdigitable.udonroad2.user.User
import twitter4j.Status
import javax.inject.Inject

@Dao
abstract class TweetDao {
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun addTweetEntities(tweet: List<TweetEntity>)

    @Query("DELETE FROM TweetEntity")
    abstract fun clear()
}

class StatusDao @Inject constructor(private val db: AppDatabase) {
    fun addStatuses(statuses: List<Status>) {
        val userDao = db.userDao()
        val tweetDao = db.tweetDao()
        val users = statuses.asSequence()
                .map { it.user }
                .distinctBy { it.id }
                .map { u ->
                    User(
                            id = u.id,
                            name = u.name,
                            screenName = u.screenName,
                            iconUrl = u.profileImageURLHttps
                    )
                }.toList()
        val tweet = statuses.map { s ->
            TweetEntity(
                    id = s.id,
                    text = s.text,
                    retweetCount = s.retweetCount,
                    favoriteCount = s.favoriteCount,
                    userId = s.user.id
            )
        }
        db.runInTransaction {
            userDao.addUsers(users)
            tweetDao.addTweetEntities(tweet)
        }
    }
}
