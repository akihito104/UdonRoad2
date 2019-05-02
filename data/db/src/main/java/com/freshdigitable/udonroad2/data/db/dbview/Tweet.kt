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

package com.freshdigitable.udonroad2.data.db.dbview

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import com.freshdigitable.udonroad2.model.TweetListItem
import org.threeten.bp.Instant

@DatabaseView(viewName = "tweet", value = """
    SELECT
     TweetEntityDb.id, text, created_at, retweet_count, favorite_count, source,
     UserEntity.id AS user_id,
     UserEntity.name AS user_name,
     UserEntity.screen_name AS user_screen_name,
     UserEntity.icon_url AS user_icon_url
    FROM TweetEntityDb
    INNER JOIN UserEntity ON TweetEntityDb.user_id = UserEntity.id
""")
data class Tweet(
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "text")
    override val text: String,

    @ColumnInfo(name = "retweet_count")
    override val retweetCount: Int,

    @ColumnInfo(name = "favorite_count")
    override val favoriteCount: Int,

    @Embedded(prefix = "user_")
    override val user: User,

    @ColumnInfo(name = "source")
    override val source: String,

    @ColumnInfo(name = "created_at")
    override val createdAt: Instant
) : com.freshdigitable.udonroad2.model.Tweet

@DatabaseView(viewName = "tweet_list_item", value = """
    WITH
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
     tweet.id AS qt_id,
     text AS qt_text,
     created_at AS qt_created_at,
     retweet_count AS qt_retweet_count,
     favorite_count AS qt_favorite_count,
     source AS qt_source,
     UserEntity.id AS qt_user_id,
     UserEntity.name AS qt_user_name,
     UserEntity.screen_name AS qt_user_screen_name,
     UserEntity.icon_url AS qt_user_icon_url
    FROM tweet
    INNER JOIN UserEntity ON tweet.user_id = UserEntity.id
    )
    SELECT tweet.*, original.*, quoted.*
    FROM structured_tweet
    INNER JOIN tweet ON tweet.id = structured_tweet.body_item_id
    INNER JOIN original ON original.original_id = structured_tweet.original_id
    LEFT OUTER JOIN quoted ON quoted.qt_id = structured_tweet.quoted_item_id
""")
data class TweetListItem(
    @ColumnInfo(name = "original_id")
    override val originalId: Long,

    @Embedded(prefix = "original_user_")
    override val originalUser: User,

    @Embedded
    override val body: Tweet,

    @Embedded(prefix = "qt_")
    override val quoted: Tweet?
) : TweetListItem
