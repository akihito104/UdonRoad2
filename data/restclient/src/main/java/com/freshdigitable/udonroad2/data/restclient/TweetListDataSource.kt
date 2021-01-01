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
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.minus
import com.freshdigitable.udonroad2.model.tweet.plus
import twitter4j.Paging
import twitter4j.Query
import twitter4j.Status
import javax.inject.Inject

class HomeTimelineDataSource @Inject constructor(
    private val twitter: AppTwitter
) : RemoteListDataSource<QueryType.TweetQueryType.Timeline, TweetEntity> {

    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType.Timeline>
    ): PagedResponseList<TweetEntity> = twitter.fetch {
        val list = query.type.userId?.value?.let { id ->
            when (query.pageOption) {
                PageOption.OnInit -> getUserTimeline(id)
                else -> getUserTimeline(id, query.pageOption.toPaging())
            }
        } ?: when (query.pageOption) {
            PageOption.OnInit -> homeTimeline
            else -> getHomeTimeline(query.pageOption.toPaging())
        }
        PagedResponseList.create(list, query)
    }
}

class FavTimelineDataSource @Inject constructor(
    private val twitter: AppTwitter
) : RemoteListDataSource<QueryType.TweetQueryType.Fav, TweetEntity> {

    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType.Fav>
    ): PagedResponseList<TweetEntity> = twitter.fetch {
        val list = query.type.userId?.value?.let { id ->
            when (query.pageOption) {
                PageOption.OnInit -> getFavorites(id)
                else -> getFavorites(id, query.pageOption.toPaging())
            }
        } ?: when (query.pageOption) {
            PageOption.OnInit -> favorites
            else -> getFavorites(query.pageOption.toPaging())
        }
        PagedResponseList.create(list, query)
    }
}

class MediaTimelineDataSource @Inject constructor(
    private val twitter: AppTwitter
) : RemoteListDataSource<QueryType.TweetQueryType.Media, TweetEntity> {
    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType.Media>
    ): PagedResponseList<TweetEntity> = twitter.fetch {
        val q = Query(query.type.query).apply {
            maxId = query.pageOption.maxId ?: -1
            sinceId = query.pageOption.sinceId ?: -1
            count = query.pageOption.count
            resultType = Query.ResultType.recent
        }
        val res = search(q).tweets
        PagedResponseList.create(res, query) // fixme: use search result to avoid redundancy request
    }
}

fun PageOption.toPaging(): Paging {
    val paging = Paging(page, count)
    sinceId?.let { paging.sinceId = it }
    maxId?.let { paging.maxId = it }
    return paging
}

// fixme
internal fun PagedResponseList.Companion.create(
    statuses: List<Status>,
    query: ListQuery<out QueryType.TweetQueryType>
): PagedResponseList<TweetEntity> {
    val list = statuses.map(Status::toEntity)
    val prependCursor = when (query.pageOption) {
        PageOption.OnInit, is PageOption.OnHead -> list.firstOrNull()?.let { it.id + 1 }
        is PageOption.OnTail -> null
    }
    val appendCursor = when (query.pageOption) {
        PageOption.OnInit, is PageOption.OnTail -> list.lastOrNull()?.let { it.id - 1 }
        is PageOption.OnHead -> null
    }
    return PagedResponseList(
        list = list,
        prependCursor = prependCursor?.value,
        appendCursor = appendCursor?.value,
    )
}
