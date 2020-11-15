package com.freshdigitable.udonroad2.data.restclient.data

import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import org.threeten.bp.Instant

internal class TweetEntityRest(
    override val id: TweetId,
    override val text: String,
    override val retweetCount: Int,
    override val favoriteCount: Int,
    override val user: User,
    override val retweetedTweet: TweetEntity?,
    override val quotedTweet: TweetEntity?,
    override val inReplyToTweetId: TweetId?,
    override val isRetweeted: Boolean,
    override val isFavorited: Boolean,
    override val possiblySensitive: Boolean,
    override val source: String,
    override val createdAt: Instant,
    override val mediaItems: List<MediaItem>,
    override val replyEntities: List<UserReplyEntity>,
) : TweetEntity

internal data class UserReplyEntityRest(
    override val userId: UserId,
    override val screenName: String,
    override val start: Int,
    override val end: Int,
) : UserReplyEntity
