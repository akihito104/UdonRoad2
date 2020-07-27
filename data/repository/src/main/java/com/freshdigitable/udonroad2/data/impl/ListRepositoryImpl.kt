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
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceProvider
import com.freshdigitable.udonroad2.data.restclient.RemoteListDataSourceProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal class ListRepositoryImpl<Q : QueryType, E>(
    private val localDataSource: LocalListDataSource<Q, E>,
    private val remoteDataSource: RemoteListDataSource<Q, E>,
    private val executor: AppExecutor
) : ListRepository<Q> {

    private val _loading = MutableLiveData<Boolean>()
    override val loading: LiveData<Boolean> = _loading

    override fun loadList(query: ListQuery<Q>, owner: String) {
        executor.launchIO {
            _loading.postValue(true)
            try {
                val timeline = remoteDataSource.getList(query)
                localDataSource.putList(timeline, query, owner)
            } catch (e: IOException) {
                Timber.tag("TweetTimelineRepository").e(e, "fetchTimeline: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    override fun clear(owner: String) {
        executor.launchIO {
            localDataSource.clean(owner)
        }
    }
}

@Singleton
class SelectedItemRepository @Inject constructor() {
    private val selectedItems: MutableMap<ListOwner<*>, SelectedItemId> = mutableMapOf()

    fun put(itemId: SelectedItemId) {
        if (itemId.originalId == null) {
            remove(itemId.owner)
        } else {
            selectedItems[itemId.owner] = itemId
        }
    }

    fun remove(owner: ListOwner<*>) {
        selectedItems.remove(owner)
    }

    fun find(owner: ListOwner<*>): SelectedItemId? {
        return selectedItems[owner]
    }
}

fun <Q : QueryType> ListRepository.Factory.create(
    query: Q,
    localListDataSourceProvider: LocalListDataSourceProvider,
    remoteListDataSourceProvider: RemoteListDataSourceProvider,
    executor: AppExecutor
): ListRepository<Q> {
    return ListRepositoryImpl<Q, Any>(
        localListDataSourceProvider.get(query),
        remoteListDataSourceProvider.get(query), executor
    )
}

internal class PagedListProviderImpl<Q : QueryType, I>(
    private val pagedListDataSourceFactory: PagedListProvider.DataSourceFactory<I>,
    private val repository: ListRepository<Q>,
    private val executor: AppExecutor
) : PagedListProvider<Q, I> {

    companion object {
        private val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(20)
            .setInitialLoadSizeHint(100)
            .build()
    }

    override fun getList(
        queryType: Q,
        owner: String,
        onEndPageOption: (I) -> PageOption
    ): LiveData<PagedList<I>> {
        val timeline = pagedListDataSourceFactory.getDataSourceFactory(owner)
        return LivePagedListBuilder(timeline, config)
            .setFetchExecutor(executor.io)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<I>() {
                override fun onZeroItemsLoaded() {
                    super.onZeroItemsLoaded()
                    repository.loadList(ListQuery(queryType), owner)
                }

                override fun onItemAtEndLoaded(itemAtEnd: I) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    repository.loadList(ListQuery(queryType, onEndPageOption(itemAtEnd)), owner)
                }
            })
            .build()
    }
}

fun <Q : QueryType, I> PagedListProvider.Factory.create(
    pagedListDataSourceFactory: PagedListProvider.DataSourceFactory<I>,
    repository: ListRepository<Q>,
    executor: AppExecutor
): PagedListProvider<Q, I> {
    return PagedListProviderImpl(pagedListDataSourceFactory, repository, executor)
}
