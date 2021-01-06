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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
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
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.ioContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

internal class ListRepositoryImpl<Q : QueryType, E : Any>(
    private val localDataSource: LocalListDataSource<Q, E>,
    private val remoteDataSource: RemoteListDataSource<Q, E>,
) : ListRepository<Q> {

    private val _loading = MutableLiveData<Boolean>()
    override val loading: LiveData<Boolean> = _loading

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
        if (_loading.value == true) {
            return
        }
        _loading.postValue(true)

        val listEntity = requireNotNull(localDataSource.findListEntity(owner))
        val pageOption = if (listEntity.hasNotFetchedYet) {
            PageOption.OnInit
        } else {
            option(listEntity)
        }
        if (pageOption == null) { // cannot load items any more
            _loading.postValue(false)
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
        } finally {
            _loading.postValue(false)
        }
    }

    override suspend fun clear(owner: ListId) {
        localDataSource.clean(owner)
    }
}

internal class PagedListProviderImpl<Q : QueryType, I : Any>(
    private val pagedListDataSourceFactory: PagedListProvider.DataSourceFactory<I>,
    private val repository: ListRepository<Q>,
    executor: AppExecutor,
) : PagedListProvider<Q, I> {

    companion object {
        private val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(20)
            .setInitialLoadSizeHint(100)
            .build()
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + executor.ioContext)

    override fun getList(
        queryType: Q,
        owner: ListId,
    ): LiveData<PagedList<I>> {
        val timeline = pagedListDataSourceFactory.getDataSourceFactory(owner)
        return LivePagedListBuilder(timeline, config)
            .setFetchExecutor { coroutineScope.launch { it.run() } }
            .setBoundaryCallback(object : PagedList.BoundaryCallback<I>() {
                override fun onZeroItemsLoaded() {
                    super.onZeroItemsLoaded()
                    coroutineScope.launch {
                        repository.loadAtFirst(queryType, owner)
                    }
                }

                override fun onItemAtEndLoaded(itemAtEnd: I) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    coroutineScope.launch {
                        repository.appendList(queryType, owner)
                    }
                }
            })
            .build()
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}
