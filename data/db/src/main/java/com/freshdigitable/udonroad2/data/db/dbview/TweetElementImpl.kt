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
import com.freshdigitable.udonroad2.data.db.dbview.TweetListItemDbView.TweetElementDbView
import com.freshdigitable.udonroad2.data.db.entity.UserReplyEntityDb
import com.freshdigitable.udonroad2.model.TweetMediaItem
import com.freshdigitable.udonroad2.model.tweet.DetailTweetElement
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import org.threeten.bp.Instant

@DatabaseView(
    viewName = "view_tweet_list_item",
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
    FROM tweet_element AS t 
    INNER JOIN view_user_item AS u ON t.user_id = u.id
    ),
    quoted AS (
    SELECT
     tweet_element.id AS qt_id,
     text AS qt_text,
     created_at AS qt_created_at,
     retweet_count AS qt_retweet_count,
     favorite_count AS qt_favorite_count,
     source AS qt_source,
     u.id AS qt_user_id,
     u.name AS qt_user_name,
     u.screen_name AS qt_user_screen_name,
     u.icon_url AS qt_user_icon_url,
     u.is_verified AS qt_user_is_verified,
     u.is_protected AS qt_user_is_protected
    FROM tweet_element
    INNER JOIN view_user_item AS u ON tweet_element.user_id = u.id
    )
    SELECT t.id, t.text, t.created_at, t.retweet_count,
     t.favorite_count, t.source,
     vu.id AS user_id, vu.name AS user_name, vu.screen_name AS user_screen_name,
     vu.icon_url AS user_icon_url, vu.is_verified AS user_is_verified, 
     vu.is_protected AS user_is_protected,
     original.*, quoted.* 
    FROM tweet
    INNER JOIN tweet_element AS t ON t.id = tweet.body_item_id
    INNER JOIN view_user_item AS vu ON t.user_id = vu.id
    INNER JOIN original ON original.original_id = tweet.original_id
    LEFT OUTER JOIN quoted ON quoted.qt_id = tweet.quoted_item_id
"""
)
internal data class TweetListItemDbView(
    @ColumnInfo(name = "original_id")
    val originalId: TweetId,

    @Embedded(prefix = "original_user_")
    val originalUser: TweetUserItemDb,

    @Embedded
    val body: TweetElementDbView,

    @Embedded(prefix = "qt_")
    val quoted: TweetElementDbView?,
) {
    internal data class TweetElementDbView(
        @ColumnInfo(name = "id")
        val id: TweetId,

        @ColumnInfo(name = "text")
        val text: String,

        @ColumnInfo(name = "retweet_count")
        val retweetCount: Int,

        @ColumnInfo(name = "favorite_count")
        val favoriteCount: Int,

        @Embedded(prefix = "user_")
        val user: TweetUserItemDb,

        @ColumnInfo(name = "source")
        val source: String,

        @ColumnInfo(name = "created_at")
        val createdAt: Instant,
    )
}

internal data class TweetListItemImpl(
    @Embedded
    private val tweetListItem: TweetListItemDbView,
    @ColumnInfo(name = "is_retweeted")
    private val isRetweeted: Boolean,
    @ColumnInfo(name = "retweet_id_by_current_user")
    private val retweetIdByCurrentUser: TweetId?,
    @ColumnInfo(name = "is_favorited")
    private val isFavorited: Boolean,
    @ColumnInfo(name = "qt_is_retweeted")
    private val isQuoteRetweeted: Boolean,
    @ColumnInfo(name = "qt_retweet_id_by_current_user")
    private val quoteRetweetIdByCurrentUser: TweetId?,
    @ColumnInfo(name = "qt_is_favorited")
    private val isQuoteFavorited: Boolean,
) : TweetListItem {

    override val originalId: TweetId
        @Ignore get() = tweetListItem.originalId

    override val originalUser: TweetUserItem
        @Ignore get() = tweetListItem.originalUser

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

    override val body: TweetElement
        @Ignore get() = TweetElementImpl(
            tweetListItem.body,
            bodyMediaItems,
            isRetweeted,
            isFavorited,
            retweetIdByCurrentUser,
        )

    override val quoted: TweetElement?
        @Ignore get() = tweetListItem.quoted?.let {
            TweetElementImpl(
                it,
                quoteMediaItems,
                isQuoteRetweeted,
                isQuoteFavorited,
                quoteRetweetIdByCurrentUser,
            )
        }
}

internal data class TweetElementImpl(
    private val tweet: TweetElementDbView,
    override val media: List<TweetMediaItem> = emptyList(),
    override val isRetweeted: Boolean,
    override val isFavorited: Boolean,
    override val retweetIdByCurrentUser: TweetId?
) : TweetElement {
    override val id: TweetId get() = tweet.id
    override val text: String get() = tweet.text
    override val retweetCount: Int get() = tweet.retweetCount
    override val favoriteCount: Int get() = tweet.favoriteCount
    override val user: TweetUserItem get() = tweet.user
    override val source: String get() = tweet.source
    override val createdAt: Instant get() = tweet.createdAt
}

internal data class DetailTweetListItemImpl(
    @Embedded
    private val tweetListItem: TweetListItemImpl,
) : DetailTweetListItem {
    @Relation(entity = UserReplyEntityDb::class, parentColumn = "id", entityColumn = "tweet_id")
    var bodyReplyEntities: List<UserReplyEntityDb> = emptyList()

    @Relation(entity = UserReplyEntityDb::class, parentColumn = "qt_id", entityColumn = "tweet_id")
    var quotedReplyEntity: List<UserReplyEntityDb> = emptyList()

    override val body: DetailTweetElement
        @Ignore get() = object : DetailTweetElement, TweetElement by tweetListItem.body {
            override val replyEntities: List<UserReplyEntity>
                get() = bodyReplyEntities
        }
    override val quoted: DetailTweetElement?
        @Ignore get() = tweetListItem.quoted?.let {
            val rep = quotedReplyEntity
            object : DetailTweetElement, TweetElement by it {
                override val replyEntities: List<UserReplyEntity> = rep
            }
        }
    override val originalId: TweetId
        @Ignore get() = tweetListItem.originalId
    override val originalUser: TweetUserItem
        @Ignore get() = tweetListItem.originalUser
}
