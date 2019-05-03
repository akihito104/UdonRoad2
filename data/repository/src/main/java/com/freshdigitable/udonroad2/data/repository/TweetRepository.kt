package com.freshdigitable.udonroad2.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import com.freshdigitable.udonroad2.model.RepositoryScope
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class TweetRepository @Inject constructor(
    private val dao: TweetDao,
    private val restClient: TweetApiClient
) {
    fun getTweetItem(id: Long): LiveData<TweetListItem?> {
        val liveData = MutableLiveData<TweetListItem?>()
        return Transformations.switchMap(dao.findTweetItem(id)) { item ->
            if (item == null) {
                fetchTweet(id)
                Transformations.map(dao.findTweet(id)) { tweet ->
                    val t = tweet ?: return@map null
                    com.freshdigitable.udonroad2.data.db.dbview.TweetListItem(
                        originalId = t.id,
                        originalUser = t.user,
                        body = t,
                        quoted = null) as TweetListItem?
                }
            } else {
                liveData.apply { value = item }
            }
        }
    }

    private fun fetchTweet(id: Long) {
        GlobalScope.launch {
            val tweet = networkAccess { restClient.fetchTweet(id) }
            diskAccess { dao.addTweet(tweet) }
        }
    }
}

@Module(includes = [
    DaoModule::class,
    TwitterModule::class
])
object TweetRepositoryModule {
    @Provides
    @RepositoryScope
    @JvmStatic
    fun provideTweetRepository(dao: TweetDao, apiClient: TweetApiClient): TweetRepository {
        return TweetRepository(dao, apiClient)
    }
}
