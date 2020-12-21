/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType

interface ListRepository<Q : QueryType> {
    val loading: LiveData<Boolean>

    fun loadList(query: ListQuery<Q>, owner: ListId)
    fun clear(owner: ListId)
}

interface PagedListProvider<Q : QueryType, I> {
    fun getList(
        queryType: Q,
        owner: ListId,
        onEndPageOption: (I) -> PageOption = { _ -> PageOption.OnTail() }
    ): LiveData<PagedList<I>>

    interface DataSourceFactory<I> {
        fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, I>
    }
}

interface LocalListDataSource<Q : QueryType, E> {
    suspend fun putList(entities: List<E>, query: ListQuery<Q>, owner: ListId)
    suspend fun clean(owner: ListId)
}

interface RemoteListDataSource<Q : QueryType, E> {
    suspend fun getList(query: ListQuery<Q>): List<E>
}
