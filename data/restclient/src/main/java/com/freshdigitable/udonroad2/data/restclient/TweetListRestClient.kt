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

import com.freshdigitable.udonroad2.model.ListQuery
import twitter4j.Paging
import twitter4j.Status
import twitter4j.Twitter
import javax.inject.Inject

class HomeTimelineClient @Inject constructor(
    private val twitter: Twitter
) : ListRestClient<ListQuery.Timeline> {

    override lateinit var query: ListQuery.Timeline

    override fun fetchTimeline(paging: Paging?): List<Status> {
        return query.userId?.let { id ->
            if (paging == null) {
                twitter.getUserTimeline(id)
            } else {
                twitter.getUserTimeline(id, paging)
            }
        } ?: if (paging == null) {
            twitter.homeTimeline
        } else {
            twitter.getHomeTimeline(paging)
        }
    }
}

class FavTimelineClient @Inject constructor(
    private val twitter: Twitter
) : ListRestClient<ListQuery.Fav> {

    override lateinit var query: ListQuery.Fav

    override fun fetchTimeline(paging: Paging?): List<Status> {
        return query.userId?.let { id ->
            if (paging == null) {
                twitter.getFavorites(id)
            } else {
                twitter.getFavorites(id, paging)
            }
        } ?: if (paging == null) {
            twitter.favorites
        } else {
            twitter.getFavorites(paging)
        }
    }
}
