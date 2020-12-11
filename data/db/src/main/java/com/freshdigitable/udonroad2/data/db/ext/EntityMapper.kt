package com.freshdigitable.udonroad2.data.db.ext

import com.freshdigitable.udonroad2.data.db.entity.MediaDbEntity
import com.freshdigitable.udonroad2.data.db.entity.MemberListEntity
import com.freshdigitable.udonroad2.data.db.entity.StructuredTweetEntity
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
import com.freshdigitable.udonroad2.data.db.entity.UrlEntity
import com.freshdigitable.udonroad2.data.db.entity.UserEntityDb
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.UrlItem
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.user.UserEntity

internal fun UserEntity.toEntity(): UserEntityDb {
    return if (this is UserEntityDb) {
        return this
    } else {
        UserEntityDb(this)
    }
}

internal fun TweetEntity.toDbEntity(): TweetEntityDb {
    return TweetEntityDb(
        id = id,
        createdAt = createdAt,
        favoriteCount = favoriteCount,
        inReplyToTweetId = inReplyToTweetId,
        isFavorited = isFavorited,
        isRetweeted = isRetweeted,
        possiblySensitive = possiblySensitive,
        quotedTweetId = quotedTweet?.id,
        retweetCount = retweetCount,
        retweetedTweetId = retweetedTweet?.id,
        source = source,
        text = text,
        userId = user.id
    )
}

internal fun TweetEntity.toStructuredTweet(): StructuredTweetEntity {
    return StructuredTweetEntity(
        originalId = id,
        bodyTweetId = retweetedTweet?.id ?: id,
        quotedTweetId = retweetedTweet?.quotedTweet?.id ?: quotedTweet?.id
    )
}

internal fun TweetEntity.toListEntity(owner: String): TweetListEntity {
    return TweetListEntity(
        originalId = id,
        order = id.value,
        owner = owner
    )
}

internal fun MediaEntity.toEntity(): MediaDbEntity {
    return MediaDbEntity(
        id = id,
        url = url,
        type = MediaType.find(type.value),
        largeSize = largeSize?.toEntity(),
        mediumSize = mediumSize?.toEntity(),
        smallSize = smallSize?.toEntity(),
        thumbSize = thumbSize?.toEntity(),
        mediaUrl = mediaUrl,
        videoAspectRatioWidth = videoAspectRatioWidth,
        videoAspectRatioHeight = videoAspectRatioHeight,
        videoDurationMillis = videoDurationMillis
    )
}

internal fun MediaEntity.Size.toEntity(): MediaDbEntity.Size {
    return MediaDbEntity.Size(width, height, resizeType)
}

internal fun UrlItem.toEntity(): UrlEntity {
    return UrlEntity(url, displayUrl, expandedUrl, start, end)
}

internal fun MemberList.toEntity(): MemberListEntity {
    return MemberListEntity(
        id = id,
        name = name,
        description = description,
        isPublic = isPublic,
        followerCount = followerCount,
        memberCount = memberCount,
        userId = user.id
    )
}
