package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.Flow

class TweetRepository(
    private val dao: TweetDao,
    private val prefs: SharedPreferenceDataSource,
    private val restClient: TweetApiClient,
) {
    fun getTweetItemSource(id: TweetId): Flow<TweetListItem?> = dao.getTweetListItemSource(id)

    suspend fun findTweetListItem(id: TweetId): TweetListItem? {
        val currentUserId = checkNotNull(prefs.getCurrentUserId())
        val tweet = restClient.fetchTweet(id)
        dao.addTweet(tweet, currentUserId)
        return dao.findTweetListItem(id)
    }

    suspend fun postLike(id: TweetId): TweetEntity {
        val currentUserId = checkNotNull(prefs.getCurrentUserId())
        try {
            val liked = restClient.postLike(id)
            dao.addTweet(liked, currentUserId)
            return liked
        } catch (ex: AppTwitterException) {
            if (ex.errorType == AppTwitterException.ErrorType.ALREADY_FAVORITED) {
                dao.updateFav(id, currentUserId, true)
            }
            throw ex
        }
    }

    // todo: already unliked error
    suspend fun postUnlike(id: TweetId): TweetEntity {
        val currentUserId = checkNotNull(prefs.getCurrentUserId())
        val unliked = restClient.postUnlike(id)
        dao.addTweet(unliked, currentUserId)
        dao.updateFav(id, currentUserId, false)
        return unliked
    }

    suspend fun postRetweet(id: TweetId): TweetEntity {
        val currentUserId = checkNotNull(prefs.getCurrentUserId())
        try {
            val retweet = restClient.postRetweet(id)

            val retweetedTweetId = checkNotNull(retweet.retweetedTweet).id
            dao.updateRetweeted(retweetedTweetId, retweet.id, currentUserId, true)
            dao.addTweet(retweet, currentUserId)
            return retweet
        } catch (ex: AppTwitterException) {
            if (ex.errorType == AppTwitterException.ErrorType.ALREADY_RETWEETED) {
                dao.updateRetweeted(id, null, currentUserId, true)
            }
            throw ex
        }
    }

    // todo: already unretweeted error
    suspend fun postUnretweet(id: TweetId): TweetEntity {
        val currentUserId = checkNotNull(prefs.getCurrentUserId())
        val retweetId = dao.findRetweetIdByTweetId(id, currentUserId)
        val unretweeted = restClient.postUnretweet(retweetId ?: id)

        val tweetId = unretweeted.retweetedTweet?.id ?: unretweeted.id
        dao.updateRetweeted(tweetId, null, currentUserId, false)
        dao.addTweet(unretweeted, currentUserId)
        return unretweeted
    }

    // todo: already deleted error
    suspend fun deleteTweet(id: TweetId): TweetEntity {
        val deleted = restClient.deleteTweet(id)
        dao.deleteTweet(id)
        return deleted
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
        prefs: SharedPreferenceDataSource,
        apiClient: TweetApiClient,
    ): TweetRepository {
        return TweetRepository(dao, prefs, apiClient)
    }
}
