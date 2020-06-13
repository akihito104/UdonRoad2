package com.freshdigitable.udonroad2.data.restclient.data

import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.User
import org.threeten.bp.Instant

internal class TweetEntityRest(
    override val id: Long,
    override val text: String,
    override val retweetCount: Int,
    override val favoriteCount: Int,
    override val user: User,
    override val retweetedTweet: TweetEntity?,
    override val quotedTweet: TweetEntity?,
    override val inReplyToTweetId: Long?,
    override val isRetweeted: Boolean,
    override val isFavorited: Boolean,
    override val possiblySensitive: Boolean,
    override val source: String,
    override val createdAt: Instant,
    override val mediaItems: List<MediaItem>
) : TweetEntity
