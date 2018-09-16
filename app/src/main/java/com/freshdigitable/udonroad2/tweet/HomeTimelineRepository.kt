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

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.di.AppExecutor
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import twitter4j.Status
import twitter4j.Twitter
import javax.inject.Inject

class HomeTimelineRepository @Inject constructor(
        private val tweetDao: TweetDao,
        private val executor: AppExecutor,
        private val twitter: Twitter
) {
    val timeline: LiveData<PagedList<Tweet>> by lazy {
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(20)
                .build()
        LivePagedListBuilder(tweetDao.getHomeTimeline(), config)
                .setFetchExecutor(executor.discExecutor)
                .setBoundaryCallback(object: PagedList.BoundaryCallback<Tweet>() {
                    override fun onZeroItemsLoaded() {
                        super.onZeroItemsLoaded()
                        fetchHomeTimeline()
                    }
                })
                .build()
    }

    private fun fetchHomeTimeline() {
        Single.create<List<Status>> { source ->
            source.onSuccess(twitter.homeTimeline)
        }
                .map { statuses ->
                    statuses.map { s ->
                        val user = User(
                                id = s.user.id,
                                name = s.user.name,
                                screenName = s.user.screenName
                        )
                        Tweet(
                                id = s.id,
                                text = s.text,
                                retweetCount = s.retweetCount,
                                favoriteCount = s.favoriteCount,
                                user = user
                        )
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe { tweets ->
                    executor.diskIO { tweetDao.addTweets(tweets) }
                }
    }
}
