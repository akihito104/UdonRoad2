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

package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Paging
import twitter4j.Query
import twitter4j.Status
import twitter4j.Twitter
import javax.inject.Inject

class HomeTimelineDataSource @Inject constructor(
    private val twitter: Twitter
) : RemoteListDataSource<QueryType.TweetQueryType.Timeline, TweetEntity> {

    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType.Timeline>
    ): List<TweetEntity> = withContext(Dispatchers.IO) {
        (
            query.type.userId?.value?.let { id ->
                when (query.pageOption) {
                    PageOption.OnInit -> twitter.getUserTimeline(id)
                    else -> twitter.getUserTimeline(id, query.pageOption.toPaging())
                }
            } ?: when (query.pageOption) {
                PageOption.OnInit -> twitter.homeTimeline
                else -> twitter.getHomeTimeline(query.pageOption.toPaging())
            }
            ).map(Status::toEntity)
    }
}

class FavTimelineDataSource @Inject constructor(
    private val twitter: Twitter
) : RemoteListDataSource<QueryType.TweetQueryType.Fav, TweetEntity> {

    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType.Fav>
    ): List<TweetEntity> = withContext(Dispatchers.IO) {
        (
            query.type.userId?.value?.let { id ->
                when (query.pageOption) {
                    PageOption.OnInit -> twitter.getFavorites(id)
                    else -> twitter.getFavorites(id, query.pageOption.toPaging())
                }
            } ?: when (query.pageOption) {
                PageOption.OnInit -> twitter.favorites
                else -> twitter.getFavorites(query.pageOption.toPaging())
            }
            ).map(Status::toEntity)
    }
}

class MediaTimelineDataSource @Inject constructor(
    private val twitter: Twitter
) : RemoteListDataSource<QueryType.TweetQueryType.Media, TweetEntity> {
    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType.Media>
    ): List<TweetEntity> = withContext(Dispatchers.IO) {
        val q = Query(query.type.query).apply {
            maxId = query.pageOption.maxId ?: -1
            sinceId = query.pageOption.sinceId ?: -1
            count = query.pageOption.count
            resultType = Query.ResultType.recent
        }
        twitter.search(q).tweets.map(Status::toEntity)
    }
}

fun PageOption.toPaging(): Paging {
    val paging = Paging(page, count)
    sinceId?.let { paging.sinceId = it }
    maxId?.let { paging.maxId = it }
    return paging
}
