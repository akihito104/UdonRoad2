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
        return twitter.homeTimeline.map { it.toEntity() }
    }

    fun loadAtTop(token: Long): List<TweetEntity> {
        val paging = Paging(1, 50, token)
        return twitter.getHomeTimeline(paging).map { it.toEntity() }
    }

    fun loadAtLast(token: Long): List<TweetEntity> {
        val paging = Paging(1, 50, 1, token)
        return twitter.getHomeTimeline(paging).map { it.toEntity() }
    }
}

class TweetApiClient @Inject constructor(
    private val twitter: Twitter
) {
    fun fetchTweet(id: Long): TweetEntity {
        return twitter.showStatus(id).toEntity()
    }
}

private fun Status.toEntity(): TweetEntity {
    return TweetEntityRest(
            id = id,
            text = text,
            retweetCount = retweetCount,
            favoriteCount = favoriteCount,
            user = UserEntityRest(
                    id = user.id,
                    name = user.name,
                    screenName = user.screenName,
                    iconUrl = user.profileImageURLHttps
            ),
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
