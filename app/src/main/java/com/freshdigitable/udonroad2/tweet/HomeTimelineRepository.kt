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
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import twitter4j.Paging
import twitter4j.Status
import twitter4j.Twitter
import java.util.concurrent.Callable
import javax.inject.Inject

class HomeTimelineRepository @Inject constructor(
        private val tweetDao: TweetDao,
        private val statusDao: StatusDao,
        private val executor: AppExecutor,
        private val twitter: Twitter
) {
    val timeline: LiveData<PagedList<Tweet>> by lazy {
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(20)
                .setInitialLoadSizeHint(100)
                .build()
        LivePagedListBuilder(tweetDao.getHomeTimeline(), config)
                .setFetchExecutor(executor.discExecutor)
                .setBoundaryCallback(object: PagedList.BoundaryCallback<Tweet>() {
                    override fun onZeroItemsLoaded() {
                        super.onZeroItemsLoaded()
                        fetchHomeTimeline(Callable { twitter.homeTimeline })
                    }

                    override fun onItemAtEndLoaded(itemAtEnd: Tweet) {
                        super.onItemAtEndLoaded(itemAtEnd)
                        val paging = Paging(1, 100, 1, itemAtEnd.id - 1)
                        fetchHomeTimeline(Callable { twitter.getHomeTimeline(paging) })
                    }
                })
                .build()
    }

    fun loadAtFront() {
        if (timeline.value?.isEmpty() != false) {
            fetchHomeTimeline(Callable { twitter.homeTimeline })
        } else {
            timeline.value?.get(0)?.let { tweet ->
                val paging = Paging(1, 100, tweet.id + 1)
                fetchHomeTimeline(Callable { twitter.getHomeTimeline(paging) })
            }
        }
    }

    private val loadingState = MutableLiveData<Boolean>()

    val loading : LiveData<Boolean>
        get() : LiveData<Boolean> = loadingState

    private fun fetchHomeTimeline(callable: Callable<List<Status>>) {
        Single.create<List<Status>> { source ->
            loadingState.postValue(true)
            try {
                val ret = callable.call()
                source.onSuccess(ret)
            } catch (e: Exception) {
                source.onError(e)
            }
        }
                .subscribeOn(Schedulers.io())
                .subscribe({ tweets ->
                    executor.diskIO { statusDao.addStatuses(tweets) }
                    loadingState.postValue(false)
                }, { t ->
                    Log.e("TAG", "fetchHomeTimeline: ", t)
                    loadingState.postValue(false)
                })
    }

    fun clear() {
        executor.diskIO { tweetDao.clear() }
    }
}
