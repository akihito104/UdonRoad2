package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException.Companion.isAlreadyLiked
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class TweetRepository @Inject constructor(
    private val dao: TweetDao,
    private val restClient: TweetApiClient,
    private val executor: AppExecutor
) {
    fun getTweetItem(id: TweetId): LiveData<TweetListItem?> {
        val liveData = MediatorLiveData<TweetListItem?>()
        liveData.addSource(dao.findTweetItemById(id)) {
            when (it) {
                null -> fetchTweet(id)
                else -> liveData.value = it
            }
        }
        return liveData
    }

    private fun fetchTweet(id: TweetId) {
        executor.launchIO {
            val tweet = restClient.fetchTweet(id)
            dao.addTweet(tweet)
        }
    }

    fun postLike(id: TweetId): AppAction<Result<TweetEntity>> {
        return AppAction.create {
            executor.launchIO {
                try {
                    val liked = restClient.postLike(id)
                    dao.addTweet(liked)
                    it.onNext(Result.success(liked))
                } catch (ex: AppTwitterException) {
                    if (ex.isAlreadyLiked) {
                        dao.updateFav(id, true)
                        it.onNext(Result.failure(ex))
                    } else {
                        it.onError(ex)
                    }
                }
            }
        }
    }

    fun postRetweet(id: TweetId): AppAction<TweetEntity> {
        return AppAction.create {
            executor.launchIO {
                val retweeted = restClient.postRetweet(id)
                dao.addTweet(retweeted)
                it.onNext(retweeted)
            }
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
        executor: AppExecutor
    ): TweetRepository {
        return TweetRepository(dao, apiClient, executor)
    }
}
