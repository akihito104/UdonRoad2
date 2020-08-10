package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.TwitterException
import javax.inject.Inject

class TweetApiClient @Inject constructor(
    private val twitter: Twitter
) {
    suspend fun fetchTweet(id: TweetId): TweetEntity = withContext(Dispatchers.IO) {
        twitter.showStatus(id.value).toEntity()
    }

    suspend fun postLike(id: TweetId): TweetEntity = withContext(Dispatchers.IO) {
        try {
            twitter.createFavorite(id.value).toEntity()
        } catch (ex: TwitterException) {
            throw AppTwitterException(ex)
        }
    }

    suspend fun postRetweet(id: TweetId): TweetEntity = withContext(Dispatchers.IO) {
        try {
            twitter.retweetStatus(id.value).toEntity()
        } catch (ex: TwitterException) {
            throw AppTwitterException(ex)
        }
    }
}
