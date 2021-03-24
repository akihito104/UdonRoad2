package com.freshdigitable.udonroad2.data.db.ext

import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineDb
import com.freshdigitable.udonroad2.data.db.entity.MediaDbEntity
import com.freshdigitable.udonroad2.data.db.entity.TweetElementDb
import com.freshdigitable.udonroad2.data.db.entity.TweetEntityDb
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
import com.freshdigitable.udonroad2.data.db.entity.UrlEntity
import com.freshdigitable.udonroad2.data.db.entity.UserEntityDb
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.TweetId
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

internal fun TweetEntity.toDbEntity(): TweetElementDb {
    return TweetElementDb(
        id = id,
        createdAt = createdAt,
        favoriteCount = favoriteCount,
        inReplyToTweetId = inReplyToTweetId,
        possiblySensitive = possiblySensitive,
        quotedTweetId = quotedTweet?.id,
        retweetCount = retweetCount,
        retweetedTweetId = retweetedTweet?.id,
        source = source,
        text = text,
        userId = user.id
    )
}

internal fun TweetEntity.toTweetEntityDb(): TweetEntityDb {
    return TweetEntityDb(
        originalId = id,
        bodyTweetId = retweetedTweet?.id ?: id,
        quotedTweetId = retweetedTweet?.quotedTweet?.id ?: quotedTweet?.id
    )
}

internal fun TweetEntity.toListEntity(owner: ListId): TweetListEntity {
    return TweetListEntity(
        originalId = id,
        listId = owner,
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

internal fun UrlItem.toEntity(tweetId: TweetId): UrlEntity =
    UrlEntity(tweetId, url, displayUrl, expandedUrl, start, end)

internal fun CustomTimelineEntity.toEntity(): CustomTimelineDb {
    return CustomTimelineDb(
        id = id,
        name = name,
        description = description,
        isPublic = isPublic,
        followerCount = followerCount,
        memberCount = memberCount,
        userId = user.id
    )
}
