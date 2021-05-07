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
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import twitter4j.Paging
import twitter4j.Query
import twitter4j.Status
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineRemoteDataSource @Inject constructor(
    private val twitter: AppTwitter,
    private val tweetApi: TweetApiClient,
) : RemoteListDataSource<QueryType.TweetQueryType, TweetEntity> {

    override suspend fun getList(
        query: ListQuery<QueryType.TweetQueryType>,
    ): PagedResponseList<TweetEntity> {
        return when (val type = query.type) {
            is QueryType.TweetQueryType.Timeline -> getTimeline(query, type)
            is QueryType.TweetQueryType.Fav -> getFavTimeline(query, type)
            is QueryType.TweetQueryType.Media -> getMediaTimeline(query, type)
            is QueryType.TweetQueryType.Conversation -> getConversationList(query, type)
            is QueryType.TweetQueryType.CustomTimeline -> twitter.fetch {
                val list = getUserListStatuses(type.id.value, query.pageOption.toPaging())
                PagedResponseList.create(list)
            }
        }
    }

    private suspend fun getTimeline(
        query: ListQuery<QueryType.TweetQueryType>,
        type: QueryType.TweetQueryType.Timeline,
    ): PagedResponseList<TweetEntity> = twitter.fetch {
        val paging = query.pageOption.toPaging()
        val list = type.userId?.let { id -> getUserTimeline(id.value, paging) }
            ?: getHomeTimeline(paging)
        PagedResponseList.create(list)
    }

    private suspend fun getFavTimeline(
        query: ListQuery<QueryType.TweetQueryType>,
        type: QueryType.TweetQueryType.Fav,
    ): PagedResponseList<TweetEntity> = twitter.fetch {
        val paging = query.pageOption.toPaging()
        val list = type.userId?.let { id -> getFavorites(id.value, paging) }
            ?: getFavorites(paging)
        PagedResponseList.create(list)
    }

    private suspend fun getMediaTimeline(
        query: ListQuery<QueryType.TweetQueryType>,
        type: QueryType.TweetQueryType.Media,
    ): PagedResponseList<TweetEntity> = twitter.fetch {
        val q = Query(type.query).apply {
            maxId = query.pageOption.maxId ?: -1
            sinceId = query.pageOption.sinceId ?: -1
            count = query.pageOption.count
            resultType = Query.ResultType.recent
        }
        val res = search(q)
        PagedResponseList(
            list = res.tweets.map(Status::toEntity),
            prependCursor = res.nextQuery()?.sinceId,
            appendCursor = res.nextQuery()?.maxId,
        )
    }

    private suspend fun getConversationList(
        query: ListQuery<QueryType.TweetQueryType>,
        type: QueryType.TweetQueryType.Conversation,
    ): PagedResponseList<TweetEntity> {
        val res = mutableListOf<TweetEntity>()
        var tweetId: TweetId? = type.tweetId
        while (tweetId != null && res.size <= query.pageOption.count) {
            val element = tweetApi.findTweetEntity(tweetId)
            res.add(element)
            tweetId = element.inReplyToTweetId
        }
        return PagedResponseList(
            res,
            prependCursor = null,
            appendCursor = res.last().inReplyToTweetId?.value
        )
    }
}

private fun PageOption.toPaging(): Paging {
    val paging = Paging(page, count)
    if (sinceId != ListEntity.CURSOR_INIT) {
        sinceId?.let { paging.sinceId = it }
    }
    if (maxId != ListEntity.CURSOR_INIT) {
        maxId?.let { paging.maxId = it }
    }
    return paging
}

internal fun PagedResponseList.Companion.create(
    statuses: List<Status>,
): PagedResponseList<TweetEntity> {
    val list = statuses.map(Status::toEntity)
    return PagedResponseList(
        list = list,
        prependCursor = list.firstOrNull()?.let { it.id.value + 1 },
        appendCursor = list.lastOrNull()?.let { it.id.value - 1 }
    )
}
