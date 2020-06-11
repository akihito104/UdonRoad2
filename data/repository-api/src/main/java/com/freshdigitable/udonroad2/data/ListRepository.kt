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
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType

interface ListRepository<Q : QueryType> {
    val loading: LiveData<Boolean>

    fun loadList(query: ListQuery<Q>, owner: String)
    fun clear(owner: String)

    companion object Factory
}

interface PagedListProvider<Q : QueryType, I> {
    fun getList(
        queryType: Q,
        owner: String,
        onEndPageOption: (I) -> PageOption = { _ -> PageOption.OnTail() }
    ): LiveData<PagedList<I>>

    interface DataSourceFactory<I> {
        fun getDataSourceFactory(owner: String): DataSource.Factory<Int, I>
    }

    companion object Factory
}

interface LocalListDataSource<Q : QueryType, E> {
    suspend fun putList(entities: List<E>, query: ListQuery<Q>? = null, owner: String? = null)
    suspend fun clean(owner: String)
}

interface RemoteListDataSource<Q : QueryType, E> {
    suspend fun getList(query: ListQuery<Q>): List<E>
}
