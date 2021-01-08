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
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.IOException

internal class ListRepositoryImpl<Q : QueryType, E : Any>(
    private val localDataSource: LocalListDataSource<Q, E>,
    private val remoteDataSource: RemoteListDataSource<Q, E>,
) : ListRepository<Q> {

    override suspend fun loadAtFirst(query: Q, owner: ListId) {
        loadList(query, owner) {
            if (it.hasNotFetchedYet) PageOption.OnInit else null
        }
    }

    override suspend fun prependList(query: Q, owner: ListId) {
        loadList(query, owner) { listEntity ->
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
        option: (ListEntity) -> PageOption?
    ) {
        val listEntity = requireNotNull(localDataSource.findListEntity(owner))
        val pageOption = if (listEntity.hasNotFetchedYet) {
            PageOption.OnInit
        } else {
            option(listEntity)
        }
        if (pageOption == null) { // cannot load items any more
            return
        }
        val q = ListQuery(queryType, pageOption)

        try {
            val timeline = remoteDataSource.getList(q)
            localDataSource.putList(timeline, q, owner)
        } catch (e: IOException) {
            // TODO: recover or notifying
            Timber.tag("ListRepository").e(e, "loadList: ${e.message}")
        } catch (e: AppTwitterException) {
            // TODO: recover or notifying
            Timber.tag("ListRepository").e(e, "loadList: ${e.message}")
        }
    }

    override suspend fun clear(owner: ListId) {
        localDataSource.clean(owner)
    }
}

internal class PagedListProviderImpl<Q : QueryType, I : Any>(
    private val pagedListDataSourceFactory: PagedListProvider.DataSourceFactory<I>,
    private val repository: ListRepository<Q>,
) : PagedListProvider<Q, I> {

    companion object {
        private val config = PagingConfig(
            enablePlaceholders = false,
            pageSize = 50,
            initialLoadSize = 50,
            prefetchDistance = 10,
        )
    }

    @ExperimentalPagingApi
    override fun getList(queryType: Q, owner: ListId): Flow<PagingData<I>> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                Timber.tag("PagedListProvider").d("source loaded")
                pagedListDataSourceFactory.getDataSourceFactory(owner)
            },
            remoteMediator = object : RemoteMediator<Int, I>() {
                override suspend fun load(
                    loadType: LoadType,
                    state: PagingState<Int, I>
                ): MediatorResult {
                    Timber.tag("PagedListProvider")
                        .d("getList: type>$loadType, state>$state")
                    when (loadType) {
                        LoadType.REFRESH -> repository.loadAtFirst(queryType, owner)
                        LoadType.APPEND -> repository.appendList(queryType, owner)
                        LoadType.PREPEND -> repository.prependList(queryType, owner)
                    }
                    return MediatorResult.Success(endOfPaginationReached = true) // FIXME
                }
            }
        ).flow
    }
}
