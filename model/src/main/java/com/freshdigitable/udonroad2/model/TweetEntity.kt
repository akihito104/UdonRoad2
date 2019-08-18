package com.freshdigitable.udonroad2.model

import org.threeten.bp.Instant

interface TweetEntity {
    val id: Long

    val text: String

    val retweetCount: Int

    val favoriteCount: Int

    val user: User

    val retweetedTweet: TweetEntity?

    val quotedTweet: TweetEntity?

    val inReplyToTweetId: Long?

    val isRetweeted: Boolean

    val isFavorited: Boolean

    val possiblySensitive: Boolean

    val source: String

    val createdAt: Instant

    val media: List<MediaItem>
}
