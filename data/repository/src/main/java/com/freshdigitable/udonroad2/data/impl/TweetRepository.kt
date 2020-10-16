package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import dagger.Module
import dagger.Provides

class TweetRepository(
    private val dao: TweetDao,
    private val restClient: TweetApiClient,
) {
    fun getTweetItemSource(id: TweetId): LiveData<TweetListItem?> = dao.getTweetListItemSource(id)

    suspend fun findTweetListItem(id: TweetId): TweetListItem? {
        val tweet = restClient.fetchTweet(id)
        dao.addTweet(tweet)
        return dao.findTweetListItem(id)
    }

    suspend fun postLike(id: TweetId): TweetEntity {
        try {
            val liked = restClient.postLike(id)
            dao.addTweet(liked)
            return liked
        } catch (ex: AppTwitterException) {
            if (ex.errorType == AppTwitterException.ErrorType.ALREADY_FAVORITED) {
                dao.updateFav(id, true)
            }
            throw ex
        }
    }

    suspend fun postRetweet(id: TweetId): TweetEntity {
        try {
            val retweeted = restClient.postRetweet(id)
            dao.addTweet(retweeted)
            return retweeted
        } catch (ex: AppTwitterException) {
            if (ex.errorType == AppTwitterException.ErrorType.ALREADY_RETWEETED) {
                dao.updateRetweeted(id, true)
            }
            throw ex
        }
    }
}

@Module(
    includes = [
        DaoModule::class
    ]
)
object TweetRepositoryModule {
    @Provides
    fun provideTweetRepository(
        dao: TweetDao,
        apiClient: TweetApiClient,
    ): TweetRepository {
        return TweetRepository(dao, apiClient)
    }
}
