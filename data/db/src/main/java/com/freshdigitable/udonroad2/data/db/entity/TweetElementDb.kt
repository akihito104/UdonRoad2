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

package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.UserId
import org.threeten.bp.Instant

@Entity(
    tableName = "tweet_element",
    foreignKeys = [
        ForeignKey(
            entity = UserEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            deferred = true
        ),
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["retweeted_tweet_id"],
            deferred = true
        )
    ],
    indices = [
        Index("user_id"),
        Index("retweeted_tweet_id")
    ]
)
internal class TweetElementDb(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: TweetId,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "retweet_count")
    val retweetCount: Int,

    @ColumnInfo(name = "favorite_count")
    val favoriteCount: Int,

    @ColumnInfo(name = "user_id")
    val userId: UserId,

    @ColumnInfo(name = "retweeted_tweet_id")
    val retweetedTweetId: TweetId?,

    @ColumnInfo(name = "quoted_tweet_id")
    val quotedTweetId: TweetId?,

    @ColumnInfo(name = "in_reply_to_tweet_id")
    val inReplyToTweetId: TweetId?,

    @ColumnInfo(name = "possibly_sensitive")
    val possiblySensitive: Boolean,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)

@Entity(
    tableName = "favorited",
    primaryKeys = ["tweet_id", "source_user_id"],
    foreignKeys = [
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["tweet_id"]
        ),
    ]
)
internal data class Favorited(
    @ColumnInfo(name = "tweet_id")
    val tweetId: TweetId,
    @ColumnInfo(name = "source_user_id", index = true)
    val sourceUserId: UserId,
)

@Entity(
    tableName = "retweeted",
    primaryKeys = ["tweet_id", "source_user_id"],
    foreignKeys = [
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["tweet_id"]
        ),
    ]
)
internal data class Retweeted(
    @ColumnInfo(name = "tweet_id")
    val tweetId: TweetId,
    @ColumnInfo(name = "source_user_id", index = true)
    val sourceUserId: UserId,
    @ColumnInfo(name = "retweet_id")
    val retweetId: TweetId?,
)

@Entity(
    tableName = "user_reply",
    primaryKeys = ["tweet_id", "start"],
    foreignKeys = [
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["tweet_id"],
            deferred = true
        ),
    ],
)
internal class UserReplyEntityDb(
    @ColumnInfo(name = "tweet_id")
    val tweetId: TweetId,

    @ColumnInfo(name = "user_id")
    override val userId: UserId,

    @ColumnInfo(name = "screen_name")
    override val screenName: String,

    @ColumnInfo(name = "start")
    override val start: Int,

    @ColumnInfo(name = "end")
    override val end: Int
) : UserReplyEntity {
    companion object
}
