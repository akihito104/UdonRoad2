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

package com.freshdigitable.udonroad2.data.impl

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.ListEntity.Companion.hasNotFetchedYet
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

internal class ListRepositoryImpl<Q : QueryType, E : Any>(
    private val localDataSource: LocalListDataSource<Q, E>,
    private val remoteDataSource: RemoteListDataSource<Q, E>,
) : ListRepository<Q, E> {

    override suspend fun loadAtFirst(query: Q, owner: ListId) {
        localDataSource.prepareList(query, owner)
    }

    override suspend fun prependList(query: Q, owner: ListId): List<E> {
        return loadList(query, owner) { listEntity ->
            when (val cursor = listEntity.prependCursor) {
                ListEntity.CURSOR_INIT -> PageOption.OnInit
                else -> PageOption.OnHead(cursor)
            }
        }
    }

    override suspend fun appendList(query: Q, owner: ListId) {
        loadList(query, owner) { listEntity ->
            val cursor = listEntity.appendCursor
            cursor?.let { PageOption.OnTail(it) }
        }
    }

    private suspend fun loadList(
        queryType: Q,
        owner: ListId,
        option: (ListEntity) -> PageOption?,
    ): List<E> {
        val listEntity = requireNotNull(findListEntity(owner)) {
            "ListEntity(owner: $owner) should be registered."
        }
        val pageOption = when {
            listEntity.hasNotFetchedYet -> PageOption.OnInit
            else -> option(listEntity) ?: return emptyList()
        }
        val q = ListQuery(queryType, pageOption)

        val timeline = remoteDataSource.getList(q)
        localDataSource.putList(timeline, q, owner)
        return timeline
    }

    override suspend fun findListEntity(owner: ListId): ListEntity? =
        localDataSource.findListEntity(owner)

    override suspend fun clear(owner: ListId) {
        localDataSource.clean(owner)
    }
}

internal class PagedListProviderImpl<Q : QueryType, I : Any>(
    private val pagedListDataSourceFactory: PagedListProvider.DataSourceFactory<I>,
    private val repository: ListRepository<Q, I>,
) : PagedListProvider<Q, I> {

    companion object {
        private val config = PagingConfig(
            enablePlaceholders = true,
            pageSize = 20,
            initialLoadSize = 20,
            prefetchDistance = 10,
        )
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getList(queryType: Q, owner: ListId): Flow<PagingData<I>> {
        return Pager(
            config = config,
            pagingSourceFactory = { pagedListDataSourceFactory.getDataSourceFactory(owner) },
            remoteMediator = getRemoteMediator(queryType, owner)
        ).flow.distinctUntilChanged()
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun getRemoteMediator(
        queryType: Q,
        owner: ListId,
    ): RemoteMediator<Int, I> = object : RemoteMediator<Int, I>() {
        override suspend fun initialize(): InitializeAction {
            repository.loadAtFirst(queryType, owner)
            return InitializeAction.SKIP_INITIAL_REFRESH
        }

        override suspend fun load(loadType: LoadType, state: PagingState<Int, I>): MediatorResult {
            Timber.tag("PagedListProvider")
                .d("getList: query>$queryType, owner>$owner type>$loadType")
            return try {
                when (loadType) {
                    LoadType.APPEND -> repository.appendList(queryType, owner)
                    LoadType.PREPEND, LoadType.REFRESH -> Unit
                }
                val listEntity = checkNotNull(repository.findListEntity(owner))
                MediatorResult.Success(endOfPaginationReached = listEntity.appendCursor == null)
            } catch (t: Exception) { // throw Error or RuntimeException
                MediatorResult.Error(t)
            }
        }
    }
}
