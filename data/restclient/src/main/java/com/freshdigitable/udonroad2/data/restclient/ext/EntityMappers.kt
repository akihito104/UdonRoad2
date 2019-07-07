package com.freshdigitable.udonroad2.data.restclient.ext

import android.graphics.Color
import com.freshdigitable.udonroad2.data.restclient.PagedResponseList
import com.freshdigitable.udonroad2.data.restclient.data.TweetEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.UserEntityRest
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.TweetEntity
import org.threeten.bp.Instant
import twitter4j.PagableResponseList
import twitter4j.Status
import twitter4j.User
import twitter4j.UserList

internal fun Status.toEntity(): TweetEntity {
    return TweetEntityRest(
        id = id,
        text = text,
        retweetCount = retweetCount,
        favoriteCount = favoriteCount,
        user = user.toEntity(),
        retweetedTweet = retweetedStatus?.toEntity(),
        quotedTweet = quotedStatus?.toEntity(),
        inReplyToTweetId = inReplyToStatusId,
        isRetweeted = isRetweeted,
        isFavorited = isFavorited,
        possiblySensitive = isPossiblySensitive,
        source = source,
        createdAt = Instant.ofEpochMilli(createdAt.time)
    )
}

internal fun User.toEntity(): UserEntityRest {
    return UserEntityRest(
        id = id,
        name = name,
        screenName = screenName,
        description = description,
        iconUrl = profileImageURLHttps,
        profileBannerImageUrl = profileBanner600x200URL,
        followerCount = followersCount,
        followingCount = friendsCount,
        tweetCount = statusesCount,
        favoriteCount = favouritesCount,
        listedCount = listedCount,
        profileLinkColor = Color.parseColor("#$profileLinkColor"),
        location = location,
        url = url,
        verified = isVerified,
        isProtected = isProtected
    )
}

internal fun UserList.toEntity(): MemberList {
    return MemberListImpl(
        id = this.id,
        name = name,
        description = description,
        user = user.toEntity(),
        memberCount = memberCount,
        followerCount = subscriberCount,
        isPublic = isPublic
    )
}

private data class MemberListImpl(
    override val user: com.freshdigitable.udonroad2.model.User,
    override val id: Long,
    override val name: String,
    override val description: String,
    override val memberCount: Int,
    override val followerCount: Int,
    override val isPublic: Boolean
) : MemberList

internal fun PagableResponseList<User>.toUserPagedList(): PagedResponseList<com.freshdigitable.udonroad2.model.User> {
    return PagedResponseList(
        list = this.map(User::toEntity),
        nextCursor = if (hasNext()) this.nextCursor else 0
    )
}

internal fun PagableResponseList<UserList>.toUserListPagedList(): PagedResponseList<MemberList> {
    return PagedResponseList(
        list = this.map { it.toEntity() },
        nextCursor = if (hasNext()) this.nextCursor else 0
    )
}
