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

import androidx.room.*
import com.freshdigitable.udonroad2.user.UserEntity

@Entity(
        foreignKeys = [
            ForeignKey(
                    entity = UserEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["user_id"],
                    deferred = true
            ),
            ForeignKey(
                    entity = TweetEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["retweeted_tweet_id"],
                    deferred = true
            ),
            ForeignKey(
                    entity = TweetEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["quoted_tweet_id"],
                    deferred = true
            )
        ],
        indices = [
            Index("user_id"),
            Index("retweeted_tweet_id"),
            Index("quoted_tweet_id")
        ]
)
class TweetEntity(
        @PrimaryKey
        @ColumnInfo(name = "id")
        val id: Long,

        @ColumnInfo(name = "text")
        val text: String,

        @ColumnInfo(name = "retweet_count")
        val retweetCount: Int,

        @ColumnInfo(name = "favorite_count")
        val favoriteCount: Int,

        @ColumnInfo(name = "user_id")
        val userId: Long,

        @ColumnInfo(name = "retweeted_tweet_id")
        val retweetedTweetId: Long?,

        @ColumnInfo(name = "quoted_tweet_id")
        val quotedTweetId: Long?,

        @ColumnInfo(name = "in_reply_to_tweet_id")
        val inReplyToTweetId: Long?,

        @ColumnInfo(name = "is_retweeted")
        val isRetweeted: Boolean,

        @ColumnInfo(name = "is_favorited")
        val isFavorited: Boolean,

        @ColumnInfo(name = "possibly_sensitive")
        val possiblySensitive: Boolean,

        @ColumnInfo(name = "source")
        val source: String
) {
    @Ignore constructor(
            id: Long,
            text: String,
            retweetCount: Int,
            favoriteCount: Int,
            user: UserEntity,
            retweetedTweet: TweetEntity?,
            quotedTweet: TweetEntity?,
            inReplyToTweetId: Long?,
            isRetweeted: Boolean,
            isFavorited: Boolean,
            possiblySensitive: Boolean,
            source: String
    ) : this(
            id, text, retweetCount, favoriteCount, user.id,
            retweetedTweet?.id, quotedTweet?.id, inReplyToTweetId, isRetweeted, isFavorited,
            possiblySensitive, source
    ) {
        this.user = user
        this.retweetedTweet = retweetedTweet
        this.quotedTweet = quotedTweet
    }

    @Ignore
    var user: UserEntity? = null

    @Ignore
    var retweetedTweet: TweetEntity? = null

    @Ignore
    var quotedTweet: TweetEntity? = null

    fun isRetweet(): Boolean = retweetedTweetId != null
}
