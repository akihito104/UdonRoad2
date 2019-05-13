package com.freshdigitable.udonroad2.data.restclient

import android.graphics.Color
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetEntity
import org.threeten.bp.Instant
import twitter4j.Paging
import twitter4j.Status
import twitter4j.User

private const val FETCH_COUNT = 50

interface ListRestClient<T : ListQuery> {
    var query: T

    fun fetchInit(): List<TweetEntity> {
        return fetchTimeline().map { it.toEntity() }
    }

    fun fetchAtTop(cursorId: Long): List<TweetEntity> {
        val paging = Paging(1, FETCH_COUNT, cursorId)
        return fetchTimeline(paging).map { it.toEntity() }
    }

    fun fetchAtBottom(cursorId: Long): List<TweetEntity> {
        val paging = Paging(1, FETCH_COUNT, 1, cursorId)
        return fetchTimeline(paging).map { it.toEntity() }
    }

    fun fetchTimeline(paging: Paging? = null): List<Status>
}

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
        profileBannerImageUrl = profileBannerURL,
        followerCount = followersCount,
        followingCount = friendsCount,
        tweetCount = statusesCount,
        favoriteCount = favouritesCount,
        listedCount = listedCount,
        profileLinkColor = Color.parseColor("#$profileLinkColor"),
        location = location,
        verified = isVerified,
        isProtected = isProtected
    )
}
