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
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import kotlinx.coroutines.flow.Flow

interface ListRepository<Q : QueryType> {
    val loading: LiveData<Boolean>

    suspend fun loadAtFirst(query: Q, owner: ListId)
    suspend fun prependList(query: Q, owner: ListId)
    suspend fun appendList(query: Q, owner: ListId)
    suspend fun clear(owner: ListId)
}

interface PagedListProvider<Q : QueryType, I : Any> {
    fun getList(queryType: Q, owner: ListId): Flow<PagingData<I>>
    fun clear()

    interface DataSourceFactory<I : Any> {
        fun getDataSourceFactory(owner: ListId): PagingSource<Int, I>
    }
}

interface LocalListDataSource<Q : QueryType, E : Any> {
    suspend fun findListEntity(id: ListId): ListEntity?
    suspend fun putList(entities: PagedResponseList<E>, query: ListQuery<Q>, owner: ListId)
    suspend fun clean(owner: ListId)
}

interface RemoteListDataSource<Q : QueryType, E : Any> {
    suspend fun getList(query: ListQuery<Q>): PagedResponseList<E>
}
