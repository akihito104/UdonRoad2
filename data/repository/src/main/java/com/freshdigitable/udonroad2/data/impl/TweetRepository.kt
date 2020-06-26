package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class TweetRepository @Inject constructor(
    private val dao: TweetDao,
    private val restClient: TweetApiClient,
    private val executor: AppExecutor
) {
    fun getTweetItem(id: Long): LiveData<TweetListItem?> {
        val liveData = MediatorLiveData<TweetListItem?>()
        liveData.addSource(dao.findTweetItemById(id)) {
            when (it) {
                null -> fetchTweet(id)
                else -> liveData.value = it
            }
        }
        return liveData
    }

    private fun fetchTweet(id: Long) {
        executor.launchIO {
            val tweet = restClient.fetchTweet(id)
            dao.addTweet(tweet)
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
