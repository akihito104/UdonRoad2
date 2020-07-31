package com.freshdigitable.udonroad2.model

interface TweetEntity : Tweet {

    override val user: User

    val retweetedTweet: TweetEntity?

    val quotedTweet: TweetEntity?

    val inReplyToTweetId: TweetId?

    val isRetweeted: Boolean

    val isFavorited: Boolean

    val possiblySensitive: Boolean
}
