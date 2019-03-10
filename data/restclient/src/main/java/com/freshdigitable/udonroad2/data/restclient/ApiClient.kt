/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.TweetEntity
import org.threeten.bp.Instant
import twitter4j.Paging
import twitter4j.Status
import twitter4j.Twitter
import javax.inject.Inject

class HomeApiClient @Inject constructor(
        private val twitter: Twitter
) {
    fun loadInit(): List<TweetEntity> {
        return twitter.homeTimeline.map { toEntity(it) }
    }

    fun loadAtTop(token: Long): List<TweetEntity> {
        val paging = Paging(1, 50, token)
        return twitter.getHomeTimeline(paging).map { toEntity(it) }
    }

    fun loadAtLast(token: Long): List<TweetEntity> {
        val paging = Paging(1, 50, 1, token)
        return twitter.getHomeTimeline(paging).map { toEntity(it) }
    }
}

fun toEntity(status: Status): TweetEntity {
    return TweetEntityRest(
            id = status.id,
            text = status.text,
            retweetCount = status.retweetCount,
            favoriteCount = status.favoriteCount,
            user = UserEntityRest(
                    id = status.user.id,
                    name = status.user.name,
                    screenName = status.user.screenName,
                    iconUrl = status.user.profileImageURLHttps
            ),
            retweetedTweet = status.retweetedStatus?.let { toEntity(it) },
            quotedTweet = status.quotedStatus?.let { toEntity(it) },
            inReplyToTweetId = status.inReplyToStatusId,
            isRetweeted = status.isRetweeted,
            isFavorited = status.isFavorited,
            possiblySensitive = status.isPossiblySensitive,
            source = status.source,
            createdAt = Instant.ofEpochMilli(status.createdAt.time)
    )
}
