package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import javax.inject.Inject

class TweetApiClient @Inject constructor(
    private val twitter: AppTwitter
) {
    suspend fun fetchTweet(id: TweetId): TweetEntity = twitter.fetch {
        showStatus(id.value).toEntity()
    }

    suspend fun postLike(id: TweetId): TweetEntity = twitter.fetch {
        createFavorite(id.value).toEntity()
    }

    suspend fun postRetweet(id: TweetId): TweetEntity = twitter.fetch {
        retweetStatus(id.value).toEntity()
    }
}
