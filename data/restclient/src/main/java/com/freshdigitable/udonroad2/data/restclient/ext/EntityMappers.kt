/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2.data.restclient.ext

import android.graphics.Color
import com.freshdigitable.udonroad2.data.restclient.PagedResponseList
import com.freshdigitable.udonroad2.data.restclient.data.MediaItemRest
import com.freshdigitable.udonroad2.data.restclient.data.MemberListImpl
import com.freshdigitable.udonroad2.data.restclient.data.SizeRest
import com.freshdigitable.udonroad2.data.restclient.data.TweetEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.UrlEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.UserEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.VideoValiantRest
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.TweetEntity
import org.threeten.bp.Instant
import twitter4j.MediaEntity
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
        createdAt = Instant.ofEpochMilli(createdAt.time),
        mediaItems = mediaEntities.map { it.toItem() }
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

fun MediaEntity.toItem(): MediaItem {
    return MediaItemRest(
        id = MediaId(id),
        mediaUrl = mediaURLHttps,
        type = MediaType.find(type),
        largeSize = sizes[MediaEntity.Size.LARGE]?.toItem(),
        mediumSize = sizes[MediaEntity.Size.MEDIUM]?.toItem(),
        smallSize = sizes[MediaEntity.Size.SMALL]?.toItem(),
        thumbSize = sizes[MediaEntity.Size.THUMB]?.toItem(),
        videoAspectRatioHeight = videoAspectRatioHeight,
        videoAspectRatioWidth = videoAspectRatioWidth,
        videoDurationMillis = videoDurationMillis,
        url = UrlEntityRest(
            text = text, displayUrl = displayURL, expandedUrl = expandedURL
        ),
        videoValiantItems = videoVariants.map(MediaEntity.Variant::toItem)
    )
}

internal fun MediaEntity.Size.toItem(): MediaItem.Size {
    return SizeRest(
        resizeType = this.resize,
        height = height,
        width = width
    )
}

internal fun MediaEntity.Variant.toItem(): VideoValiantRest {
    return VideoValiantRest(
        bitrate = bitrate,
        contentType = contentType,
        url = url
    )
}
