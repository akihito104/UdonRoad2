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
import com.freshdigitable.udonroad2.data.restclient.data.CustomTimelineEntityImpl
import com.freshdigitable.udonroad2.data.restclient.data.MediaEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.SizeRest
import com.freshdigitable.udonroad2.data.restclient.data.TweetEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.UserEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.UserReplyEntityRest
import com.freshdigitable.udonroad2.data.restclient.data.VideoValiantRest
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.CustomTimelineId
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UrlItem
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.UserEntity
import org.threeten.bp.Instant
import twitter4j.MediaEntity.Size.LARGE
import twitter4j.MediaEntity.Size.MEDIUM
import twitter4j.MediaEntity.Size.SMALL
import twitter4j.MediaEntity.Size.THUMB
import twitter4j.PagableResponseList
import twitter4j.Status
import twitter4j.TwitterResponse
import twitter4j.URLEntity
import twitter4j.UserList
import twitter4j.UserMentionEntity

internal fun Status.toEntity(
    retweetIdForRetweetedStatus: TweetId? = if (currentUserRetweetId > -1L)
        TweetId(currentUserRetweetId) else null,
): TweetEntity {
    val retweetId = if (retweetedStatus == null) retweetIdForRetweetedStatus else null
    val inReplyToTweetId = if (inReplyToStatusId > -1) TweetId(inReplyToStatusId) else null
    return TweetEntityRest(
        id = TweetId(id),
        text = text,
        retweetCount = retweetCount,
        favoriteCount = favoriteCount,
        user = user.toEntity(),
        retweetedTweet = retweetedStatus?.toEntity(retweetIdForRetweetedStatus),
        quotedTweet = quotedStatus?.toEntity(),
        inReplyToTweetId = inReplyToTweetId,
        isRetweeted = isRetweeted,
        isFavorited = isFavorited,
        possiblySensitive = isPossiblySensitive,
        source = source,
        createdAt = Instant.ofEpochMilli(createdAt.time),
        media = mediaEntities.map { it.toItem() },
        replyEntities = userMentionEntities.map { UserReplyEntity.create(it) },
        retweetIdByCurrentUser = retweetId,
        urlItems = urlEntities.map { UrlItem.create(it) }
    )
}

fun UserReplyEntity.Companion.create(
    entity: UserMentionEntity
): UserReplyEntity =
    UserReplyEntityRest(UserId(entity.id), entity.screenName, entity.start, entity.end)

fun twitter4j.User.toEntity(): UserEntity {
    return UserEntityRest(
        id = UserId(id),
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
        isVerified = isVerified,
        isProtected = isProtected
    )
}

internal fun UserList.toEntity(): CustomTimelineEntity {
    return CustomTimelineEntityImpl(
        id = CustomTimelineId(this.id),
        name = name,
        description = description,
        user = user.toEntity(),
        memberCount = memberCount,
        followerCount = subscriberCount,
        isPublic = isPublic
    )
}

internal fun <I : TwitterResponse, O> PagableResponseList<I>.toPagedResponseList(
    mapper: (I) -> O
): PagedResponseList<O> = PagedResponseList(
    list = this.map(mapper),
    appendCursor = if (this.hasNext()) this.nextCursor else ListEntity.CURSOR_REACH_TO_END,
    prependCursor = if (this.hasPrevious()) this.previousCursor else ListEntity.CURSOR_REACH_TO_END,
)

internal fun twitter4j.MediaEntity.toItem(): MediaEntityRest {
    return MediaEntityRest(
        id = MediaId(id),
        mediaUrl = mediaURLHttps,
        type = MediaType.find(type),
        largeSize = sizes[LARGE]?.toItem(),
        mediumSize = sizes[MEDIUM]?.toItem(),
        smallSize = sizes[SMALL]?.toItem(),
        thumbSize = sizes[THUMB]?.toItem(),
        videoAspectRatioHeight = videoAspectRatioHeight,
        videoAspectRatioWidth = videoAspectRatioWidth,
        videoDurationMillis = videoDurationMillis,
        url = text,
        start = start,
        end = end,
        videoValiantItems = videoVariants.map(twitter4j.MediaEntity.Variant::toItem)
    )
}

internal fun twitter4j.MediaEntity.Size.toItem(): MediaEntity.Size {
    return SizeRest(
        resizeType = this.resize,
        height = height,
        width = width
    )
}

internal fun twitter4j.MediaEntity.Variant.toItem(): VideoValiantRest {
    return VideoValiantRest(
        bitrate = bitrate,
        contentType = contentType,
        url = url
    )
}

internal fun UrlItem.Companion.create(item: URLEntity): UrlItem = object : UrlItem {
    override val url: String get() = item.url
    override val displayUrl: String get() = item.displayURL
    override val expandedUrl: String get() = item.expandedURL
    override val start: Int get() = item.start
    override val end: Int get() = item.end
}
