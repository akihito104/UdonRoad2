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
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import timber.log.Timber
import java.io.IOException

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
                // TODO: recover or notifying
                Timber.tag("ListRepository").e(e, "loadList: ${e.message}")
            } catch (e: AppTwitterException) {
                // TODO: recover or notifying
                Timber.tag("ListRepository").e(e, "loadList: ${e.message}")
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
