package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.TweetDataSource
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetEntityUpdatable

class TweetRepository(
    private val local: TweetDataSource.Local,
    private val restClient: TweetDataSource.Remote,
) : TweetDataSource by local {

    override suspend fun findTweetEntity(tweetId: TweetId): TweetEntity? {
        val entity = restClient.findTweetEntity(tweetId)
        entity?.let { local.addTweetEntity(it) }
        return entity
    }

    override suspend fun findDetailTweetItem(id: TweetId): DetailTweetListItem? {
        val tweet = restClient.findTweetEntity(id)
        tweet?.let { local.addTweetEntity(it) }
        return local.findDetailTweetItem(id)
    }

    override suspend fun updateLike(id: TweetId, isLiked: Boolean): TweetEntityUpdatable {
        return when (isLiked) {
            true -> postLike(id)
            false -> postUnlike(id)
        }
    }

    private suspend fun postLike(id: TweetId): TweetEntityUpdatable {
        try {
            val liked = restClient.updateLike(id, true)
            local.updateTweet(liked)
            return liked
        } catch (ex: AppTwitterException) {
            if (ex.errorType == AppTwitterException.ErrorType.ALREADY_FAVORITED) {
                local.updateLike(id, true)
            }
            throw ex
        }
    }

    // todo: already unliked error
    private suspend fun postUnlike(id: TweetId): TweetEntityUpdatable {
        val unliked = restClient.updateLike(id, false)
        local.updateTweet(unliked)
        local.updateLike(id, false)
        return unliked
    }

    override suspend fun updateRetweet(id: TweetId, isRetweeted: Boolean): TweetEntityUpdatable {
        return when (isRetweeted) {
            true -> postRetweet(id)
            false -> postUnretweet(id)
        }
    }

    private suspend fun postRetweet(id: TweetId): TweetEntityUpdatable {
        try {
            val retweet = restClient.updateRetweet(id, true)
            local.updateTweet(retweet)
            return retweet
        } catch (ex: AppTwitterException) {
            if (ex.errorType == AppTwitterException.ErrorType.ALREADY_RETWEETED) {
                local.updateRetweet(id, false)
            }
            throw ex
        }
    }

    // todo: already unretweeted error
    private suspend fun postUnretweet(id: TweetId): TweetEntityUpdatable {
        val unretweeted = restClient.updateRetweet(id, false)

        val tweetId = unretweeted.retweetedTweet?.id ?: unretweeted.id
        local.updateTweet(unretweeted)
        local.updateRetweet(tweetId, false)
        return unretweeted
    }

    // todo: already deleted error
    override suspend fun deleteTweet(id: TweetId) {
        restClient.deleteTweet(id)
        local.deleteTweet(id)
    }
}
