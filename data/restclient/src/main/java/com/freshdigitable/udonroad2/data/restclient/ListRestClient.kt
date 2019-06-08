package com.freshdigitable.udonroad2.data.restclient

import android.graphics.Color
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetEntity
import org.threeten.bp.Instant
import twitter4j.Paging
import twitter4j.Status
import twitter4j.User

private const val FETCH_COUNT = 50

interface ListRestClient<Q : ListQuery, E> {
    var query: Q

    suspend fun fetchInit(): List<E> {
        return fetchTimeline()
    }

    suspend fun fetchAtTop(cursorId: Long): List<E> {
        val paging = Paging(1, FETCH_COUNT, cursorId)
        return fetchTimeline(paging)
    }

    suspend fun fetchAtBottom(cursorId: Long): List<E> {
        val paging = Paging(1, FETCH_COUNT, 1, cursorId)
        return fetchTimeline(paging)
    }

    suspend fun fetchTimeline(paging: Paging? = null): List<E>
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
