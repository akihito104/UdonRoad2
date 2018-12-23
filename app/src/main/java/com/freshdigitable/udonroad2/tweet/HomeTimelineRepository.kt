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

package com.freshdigitable.udonroad2.tweet

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.di.AppExecutor
import com.freshdigitable.udonroad2.di.diskAccess
import com.freshdigitable.udonroad2.di.networkAccess
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeTimelineRepository @Inject constructor(
        private val tweetDao: TweetDao,
        private val apiClient: HomeApiClient,
        executor: AppExecutor
) {
    companion object {
        private val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(20)
                .setInitialLoadSizeHint(100)
                .build()
    }

    val timeline: LiveData<PagedList<TweetListItem>> by lazy {
        LivePagedListBuilder(tweetDao.getHomeTimeline("home"), config)
                .setFetchExecutor(executor.network)
                .setBoundaryCallback(object : PagedList.BoundaryCallback<TweetListItem>() {
                    override fun onZeroItemsLoaded() {
                        super.onZeroItemsLoaded()
                        fetchHomeTimeline { loadInit() }
                    }

                    override fun onItemAtEndLoaded(itemAtEnd: TweetListItem) {
                        super.onItemAtEndLoaded(itemAtEnd)
                        fetchHomeTimeline { loadAtLast(itemAtEnd.originalId - 1) }
                    }
                })
                .build()
    }

    fun loadAtFront() {
        timeline.value?.getOrNull(0)?.let {
            fetchHomeTimeline { loadAtTop(it.originalId + 1) }
        } ?: fetchHomeTimeline { loadInit() }
    }

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private fun fetchHomeTimeline(
            block: HomeApiClient.() -> List<TweetEntity>
    ) = GlobalScope.launch {
        _loading.postValue(true)
        try {
            val timeline = networkAccess { block(apiClient) }
            diskAccess { tweetDao.addTweets(timeline) }
        } catch (e: Exception) {
            Log.e("HomeTimelineRepository", "fetchHomeTimeline: ", e)
        } finally {
            withContext(NonCancellable) {
                _loading.postValue(false)
            }
        }
    }

    fun clear() = diskAccess { tweetDao.clear() }
}