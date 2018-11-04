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
import com.freshdigitable.udonroad2.user.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import twitter4j.Paging
import twitter4j.Status
import twitter4j.Twitter
import java.util.concurrent.Callable
import javax.inject.Inject

class HomeTimelineRepository @Inject constructor(
        private val tweetDao: TweetDao,
        private val executor: AppExecutor,
        private val twitter: Twitter
) {
    val timeline: LiveData<PagedList<TweetListItem>> by lazy {
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(20)
                .setInitialLoadSizeHint(100)
                .build()
        LivePagedListBuilder(tweetDao.getHomeTimeline("home"), config)
                .setFetchExecutor(executor.network)
                .setBoundaryCallback(object: PagedList.BoundaryCallback<TweetListItem>() {
                    override fun onZeroItemsLoaded() {
                        super.onZeroItemsLoaded()
                        fetchHomeTimeline(Callable { twitter.homeTimeline })
                    }

                    override fun onItemAtEndLoaded(itemAtEnd: TweetListItem) {
                        super.onItemAtEndLoaded(itemAtEnd)
                        val paging = Paging(1, 50, 1, itemAtEnd.originalId - 1)
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
                val paging = Paging(1, 50, tweet.originalId + 1)
                fetchHomeTimeline(Callable { twitter.getHomeTimeline(paging) })
            }
        }
    }

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private fun fetchHomeTimeline(callable: Callable<List<Status>>) = GlobalScope.launch {
        _loading.postValue(true)
        try {
            val timeline = fetch(callable)
            executor.diskIO { tweetDao.addTweets(timeline) }
        } catch (e: Exception) {
            Log.e("HomeTimelineRepository", "fetchHomeTimeline: ", e)
        } finally {
            withContext(NonCancellable) {
                _loading.postValue(false)
            }
        }
    }

    private suspend fun fetch(callable: Callable<List<Status>>): List<TweetEntity> = coroutineScope {
        GlobalScope.async(Dispatchers.Default) {
            callable.call()
                    .map { TweetEntityConverter.toEntity(it) }
        }.await()
    }

    fun clear() = executor.diskIO { tweetDao.clear() }
}

class TweetEntityConverter {
    companion object {
        @JvmStatic
        fun toEntity(status: Status): TweetEntity {
            return TweetEntity(
                    id = status.id,
                    text = status.text,
                    retweetCount = status.retweetCount,
                    favoriteCount = status.favoriteCount,
                    user = UserEntity(
                            id = status.user.id,
                            name = status.user.name,
                            screenName = status.user.screenName,
                            iconUrl = status.user.profileImageURLHttps
                    ),
                    retweetedTweet = status.retweetedStatus?.let { toEntity(it) },
                    quotedTweetId = status.quotedStatusId,
                    quotedTweet = status.quotedStatus?.let { toEntity(it) },
                    inReplyToTweetId = status.inReplyToStatusId,
                    isRetweeted = status.isRetweeted,
                    isFavorited = status.isFavorited,
                    possiblySensitive = status.isPossiblySensitive,
                    source = status.source,
                    createdAt = Instant.ofEpochMilli(status.createdAt.time)
            )
        }
    }
}