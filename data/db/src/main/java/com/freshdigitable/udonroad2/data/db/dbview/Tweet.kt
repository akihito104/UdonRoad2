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
import androidx.room.Ignore
import androidx.room.Relation
import com.freshdigitable.udonroad2.model.TweetMediaItem
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetingUser
import org.threeten.bp.Instant

@DatabaseView(
    viewName = "view_tweet",
    value =
    """
    SELECT
     t.id, text, created_at, is_retweeted, retweet_count, is_favorited, favorite_count, source, 
     u.id AS user_id,
     u.name AS user_name,
     u.screen_name AS user_screen_name,
     u.icon_url AS user_icon_url,
     u.is_verified AS user_is_verified,
     u.is_protected AS user_is_protected
    FROM tweet AS t 
    INNER JOIN view_user_item AS u ON t.user_id = u.id
"""
)
internal data class TweetDbView(
    @ColumnInfo(name = "id")
    val id: TweetId,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "is_retweeted")
    val isRetweeted: Boolean,

    @ColumnInfo(name = "retweet_count")
    val retweetCount: Int,

    @ColumnInfo(name = "is_favorited")
    val isFavorited: Boolean,

    @ColumnInfo(name = "favorite_count")
    val favoriteCount: Int,

    @Embedded(prefix = "user_")
    val user: TweetingUserDb,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)

internal data class Tweet(
    @Embedded
    val tweet: TweetDbView
) : Tweet {
    @Ignore
    override val id: TweetId = tweet.id

    @Ignore
    override val text: String = tweet.text

    @Ignore
    override val isRetweeted: Boolean = tweet.isRetweeted

    @Ignore
    override val retweetCount: Int = tweet.retweetCount

    @Ignore
    override val isFavorited: Boolean = tweet.isFavorited

    @Ignore
    override val favoriteCount: Int = tweet.favoriteCount

    @Ignore
    override val user: TweetingUser = tweet.user

    @Ignore
    override val source: String = tweet.source

    @Ignore
    override val createdAt: Instant = tweet.createdAt

    @Relation(
        entity = TweetItemMediaDbView::class, parentColumn = "id", entityColumn = "tweet_id"
    )
    override var media: List<TweetMediaItem> = emptyList()
}

@DatabaseView(
    viewName = "tweet_list_item",
    value =
    """
    WITH
    original AS (
    SELECT
     t.id AS original_id,
     u.id AS original_user_id,
     u.name AS original_user_name,
     u.screen_name AS original_user_screen_name,
     u.icon_url AS original_user_icon_url,
     u.is_verified AS original_user_is_verified,
     u.is_protected AS original_user_is_protected
    FROM tweet AS t 
    INNER JOIN view_user_item AS u ON t.user_id = u.id
    ),
    quoted AS (
    SELECT
     tweet.id AS qt_id,
     text AS qt_text,
     created_at AS qt_created_at,
     is_retweeted AS qt_is_retweeted,
     retweet_count AS qt_retweet_count,
     is_favorited AS qt_is_favorited,
     favorite_count AS qt_favorite_count,
     source AS qt_source,
     u.id AS qt_user_id,
     u.name AS qt_user_name,
     u.screen_name AS qt_user_screen_name,
     u.icon_url AS qt_user_icon_url,
     u.is_verified AS qt_user_is_verified,
     u.is_protected AS qt_user_is_protected
    FROM tweet
    INNER JOIN view_user_item AS u ON tweet.user_id = u.id
    )
    SELECT t.*, original.*, quoted.* 
    FROM structured_tweet
    INNER JOIN view_tweet AS t ON t.id = structured_tweet.body_item_id
    INNER JOIN view_user_item AS vu ON t.user_id = vu.id
    INNER JOIN original ON original.original_id = structured_tweet.original_id
    LEFT OUTER JOIN quoted ON quoted.qt_id = structured_tweet.quoted_item_id
"""
)
internal data class TweetListItemDbView(
    @ColumnInfo(name = "original_id")
    val originalId: TweetId,

    @Embedded(prefix = "original_user_")
    val originalUser: TweetingUserDb,

    @Embedded
    val body: TweetDbView,

    @Embedded(prefix = "qt_")
    val quoted: TweetDbView?
)

internal data class TweetListItem(
    @Embedded
    val tweetListItem: TweetListItemDbView
) : TweetListItem {
    @Ignore
    override val originalId: TweetId = tweetListItem.originalId

    @Ignore
    override val originalUser: TweetingUser = tweetListItem.originalUser

    @Relation(entity = TweetItemMediaDbView::class, parentColumn = "id", entityColumn = "tweet_id")
    var bodyMediaItems: List<TweetItemMediaDbView> = emptyList()
        set(value) {
            field = value.sortedBy { it.order }
        }

    @Relation(
        entity = TweetItemMediaDbView::class,
        parentColumn = "qt_id",
        entityColumn = "tweet_id"
    )
    var quoteMediaItems: List<TweetItemMediaDbView> = emptyList()
        set(value) {
            field = value.sortedBy { it.order }
        }

    override val body: Tweet
        @Ignore get() = Tweet(tweetListItem.body).apply { media = bodyMediaItems }

    override val quoted: Tweet?
        @Ignore get() = if (tweetListItem.quoted != null) {
            Tweet(tweetListItem.quoted).apply { media = quoteMediaItems }
        } else {
            null
        }
}
