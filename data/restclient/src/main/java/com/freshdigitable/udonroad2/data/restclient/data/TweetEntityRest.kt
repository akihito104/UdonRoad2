package com.freshdigitable.udonroad2.data.restclient.data

import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.UserEntity
import org.threeten.bp.Instant

internal class TweetEntityRest(
    override val id: TweetId,
    override val text: String,
    override val retweetCount: Int,
    override val favoriteCount: Int,
    override val user: UserEntity,
    override val retweetedTweet: TweetEntity?,
    override val quotedTweet: TweetEntity?,
    override val inReplyToTweetId: TweetId?,
    override val isRetweeted: Boolean,
    override val isFavorited: Boolean,
    override val possiblySensitive: Boolean,
    override val source: String,
    override val createdAt: Instant,
    override val media: List<MediaEntity>,
    override val replyEntities: List<UserReplyEntity>,
    override val retweetIdByCurrentUser: TweetId?,
) : TweetEntity

internal data class UserReplyEntityRest(
    override val userId: UserId,
    override val screenName: String,
    override val start: Int,
    override val end: Int,
) : UserReplyEntity
