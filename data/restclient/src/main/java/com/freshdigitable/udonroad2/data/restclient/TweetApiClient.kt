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
        twitter.retweetStatus(id.value).toEntity()
    }
}

class AppTwitterException constructor(exception: TwitterException) : Exception(exception) {
    val statusCode: Int = exception.statusCode
    val errorCode: Int = exception.errorCode
    val errorType: ErrorType? = findByCode(statusCode, errorCode)
    val exceptionCode: String = exception.exceptionCode

    companion object {
        private const val STATUS_FORBIDDEN: Int = 403

        private fun findByCode(statusCode: Int, errorCode: Int): ErrorType? {
            return ErrorType.values().first {
                it.statusCode == statusCode && it.errorCode == errorCode
            }
        }
    }

    enum class ErrorType(
        val statusCode: Int,
        val errorCode: Int
    ) {
        // https://developer.twitter.com/ja/docs/basics/response-codes
        ALREADY_FAVORITED(STATUS_FORBIDDEN, 139),
        ALREADY_RETWEETED(STATUS_FORBIDDEN, 327),
    }
}
