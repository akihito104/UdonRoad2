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

package com.freshdigitable.udonroad2.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.TweetDao
import com.freshdigitable.udonroad2.data.restclient.ListRestClientProvider
import com.freshdigitable.udonroad2.data.restclient.TweetListRestClient
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.RepositoryScope
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RepositoryScope
class TweetTimelineRepository(
    private val tweetDao: TweetDao,
    private val clientProvider: ListRestClientProvider,
    private val executor: AppExecutor
) : TimelineRepository, TimelineFetcher<TweetListRestClient<ListQuery>> by TweetTimelineFetcher() {

    private val _loading = MutableLiveData<Boolean>()
    override val loading: LiveData<Boolean> = _loading

    companion object {
        private val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(20)
            .setInitialLoadSizeHint(100)
            .build()
    }

    private val owner = MutableLiveData<String>()
    private val listTable: MutableMap<String, LiveData<PagedList<TweetListItem>>> = mutableMapOf()

    private val timeline: LiveData<PagedList<TweetListItem>> = Transformations.switchMap(owner) {
        listTable.getOrPut(it) { getPagedList(it) }
    }

    private lateinit var apiClient: TweetListRestClient<ListQuery>

    override fun getTimeline(owner: String, query: ListQuery): LiveData<PagedList<TweetListItem>> {
        apiClient = clientProvider.get(query)
        this.owner.value = owner
        return timeline
    }

    private fun getPagedList(owner: String): LiveData<PagedList<TweetListItem>> {
        val timeline = tweetDao.getTimeline(owner).map { it as TweetListItem }
        return LivePagedListBuilder(timeline, config)
            .setFetchExecutor(executor.disk)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<TweetListItem>() {
                override fun onZeroItemsLoaded() {
                    super.onZeroItemsLoaded()
                    fetchTimeline(owner, fetchOnZeroItems)
                }

                override fun onItemAtEndLoaded(itemAtEnd: TweetListItem) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    fetchTimeline(owner, fetchOnBottom(itemAtEnd))
                }
            })
            .build()
    }

    override fun loadAtFront() {
        val owner = requireNotNull(this.owner.value) {
            "owner should be set before calling loadAtFront()."
        }

        val item = timeline.value?.getOrNull(0)
        if (item != null) {
            fetchTimeline(owner, fetchOnTop(item))
        } else {
            fetchTimeline(owner, fetchOnZeroItems)
        }
    }

    private fun fetchTimeline(
        owner: String,
        block: suspend TweetListRestClient<ListQuery>.() -> List<TweetEntity>
    ) = GlobalScope.launch(Dispatchers.Default) {
        _loading.postValue(true)
        runCatching {
            val timeline = block(apiClient)
            diskAccess { tweetDao.addTweets(timeline, owner) }
        }.onSuccess {
            _loading.postValue(false)
        }.onFailure { e ->
            Log.e("TweetTimelineRepository", "fetchTimeline: ", e)
        }
    }

    override fun clear() {
        diskAccess {
            listTable.keys.forEach { tweetDao.clear(it) }
        }
    }
}

@Module(includes = [
    DaoModule::class,
    TwitterModule::class
])
object TimelineRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideTweetTimelineRepository(
        tweetDao: TweetDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): TweetTimelineRepository {
        return TweetTimelineRepository(tweetDao, clientProvider, executor)
    }
}
