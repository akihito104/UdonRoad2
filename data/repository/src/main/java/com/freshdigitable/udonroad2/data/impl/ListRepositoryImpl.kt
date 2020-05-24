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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.MemberListListDao
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.db.dao.UserListDao
import com.freshdigitable.udonroad2.data.restclient.ListRestClient
import com.freshdigitable.udonroad2.data.restclient.ListRestClientProvider
import com.freshdigitable.udonroad2.data.restclient.MemberListClientModule
import com.freshdigitable.udonroad2.data.restclient.TweetTimelineClientModule
import com.freshdigitable.udonroad2.data.restclient.UserListClientModule
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.RepositoryScope
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.model.UserListItem
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class ListRepositoryImpl<E, I>(
    private val localDataSource: LocalListDataSource<E, I>,
    private val fetcher: ListFetcher<ListQuery, E, ListRestClient<ListQuery, E>, I>,
    private val clientProvider: ListRestClientProvider,
    private val executor: AppExecutor
) : ListRepository<I> {

    private val _loading = MutableLiveData<Boolean>()
    override val loading: LiveData<Boolean> = _loading

    companion object {
        private val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(20)
            .setInitialLoadSizeHint(100)
            .build()
    }

    private val owner = MutableLiveData<String>()
    private val listTable: MutableMap<String, LiveData<PagedList<I>>> = mutableMapOf()

    private val timeline: LiveData<PagedList<I>> = owner.switchMap {
        listTable.getOrPut(it) { getPagedList(it) }
    }

    private lateinit var apiClient: ListRestClient<ListQuery, E>

    override fun getList(owner: String, query: ListQuery): LiveData<PagedList<I>> {
        apiClient = clientProvider.get(query)
        this.owner.value = owner
        return timeline
    }

    private fun getPagedList(owner: String): LiveData<PagedList<I>> {
        val timeline = localDataSource.getDataSourceFactory(owner)
        return LivePagedListBuilder(timeline, config)
            .setFetchExecutor(executor.disk)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<I>() {
                override fun onZeroItemsLoaded() {
                    super.onZeroItemsLoaded()
                    fetchTimeline(owner, fetcher.fetchOnZeroItems)
                }

                override fun onItemAtEndLoaded(itemAtEnd: I) {
                    super.onItemAtEndLoaded(itemAtEnd)
                    fetchTimeline(owner, fetcher.fetchOnBottom(itemAtEnd))
                }
            })
            .build()
    }

    override fun loadAtFront() {
        val owner = requireNotNull(this.owner.value) {
            "owner should be set before calling loadAtFront()."
        }

        val item = timeline.value?.getOrNull(0)
        if (item != null) {
            fetchTimeline(owner, fetcher.fetchOnTop(item))
        } else {
            fetchTimeline(owner, fetcher.fetchOnZeroItems)
        }
    }

    private fun fetchTimeline(
        owner: String,
        block: suspend ListRestClient<ListQuery, E>.() -> List<E>
    ) = GlobalScope.launch {
        _loading.postValue(true)
        runCatching {
            val timeline = block(apiClient)
            localDataSource.putList(timeline, owner)
        }.onSuccess {
            _loading.postValue(false)
        }.onFailure { e ->
            _loading.postValue(false)
            Log.e("TweetTimelineRepository", "fetchTimeline: ${e.message}", e)
        }
    }

    override fun clear() {
        diskAccess {
            listTable.keys.forEach { localDataSource.clean(it) }
        }
    }
}

@RepositoryScope
class TweetTimelineRepository(
    tweetDao: LocalListDataSource<TweetEntity, TweetListItem>,
    fetcher: ListFetcher<ListQuery, TweetEntity, ListRestClient<ListQuery, TweetEntity>, TweetListItem>,
    clientProvider: ListRestClientProvider,
    executor: AppExecutor
) : ListRepositoryImpl<TweetEntity, TweetListItem>(tweetDao, fetcher, clientProvider, executor)

@RepositoryScope
class UserListRepository(
    userDao: LocalListDataSource<User, UserListItem>,
    fetcher: ListFetcher<ListQuery, User, ListRestClient<ListQuery, User>, UserListItem>,
    clientProvider: ListRestClientProvider,
    executor: AppExecutor
) : ListRepositoryImpl<User, UserListItem>(userDao, fetcher, clientProvider, executor)

@RepositoryScope
class MemberListListRepository(
    memberListDao: LocalListDataSource<MemberList, MemberListItem>,
    fetcher: ListFetcher<ListQuery, MemberList, ListRestClient<ListQuery, MemberList>, MemberListItem>,
    clientProvider: ListRestClientProvider,
    executor: AppExecutor
) : ListRepositoryImpl<MemberList, MemberListItem>(memberListDao, fetcher, clientProvider, executor)

@Module(
    includes = [
        DaoModule::class,
        TweetTimelineClientModule::class
    ]
)
object TimelineRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideTweetTimelineRepository(
        dao: TweetListDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): TweetTimelineRepository {
        return TweetTimelineRepository(dao, TweetTimelineFetcher(), clientProvider, executor)
    }
}

@Module(
    includes = [
        DaoModule::class,
        UserListClientModule::class
    ]
)
object UserListRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideUserListRepository(
        dao: UserListDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): UserListRepository {
        return UserListRepository(dao, UserListFetcher(), clientProvider, executor)
    }
}

@Module(
    includes = [
        DaoModule::class,
        MemberListClientModule::class
    ]
)
object MemberListListRepositoryModule {
    @Provides
    @JvmStatic
    @RepositoryScope
    fun provideMemberListListRepository(
        dao: MemberListListDao,
        clientProvider: ListRestClientProvider,
        executor: AppExecutor
    ): MemberListListRepository {
        return MemberListListRepository(dao, MemberListListFetcher(), clientProvider, executor)
    }
}
