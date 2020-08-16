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

package com.freshdigitable.udonroad2.timeline.di

import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceProvider
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryProvider
import com.freshdigitable.udonroad2.data.impl.AppExecutor
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.data.restclient.RemoteListDataSourceProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.UserListItem
import com.freshdigitable.udonroad2.timeline.viewmodel.FragmentContainerViewStateModel
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module
interface TimelineViewModelModule {
    companion object {
        @Provides
        fun provideTimelineViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            viewStateModel: FragmentContainerViewStateModel,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): TimelineViewModel {
            val o = owner as ListOwner<QueryType.TweetQueryType>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<QueryType.TweetQueryType, TweetListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return TimelineViewModel(
                o,
                eventDispatcher,
                viewStateModel,
                repository,
                pagedListProvider
            )
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.TweetQueryType::class)
        fun provideTimelineViewModelKClass(): KClass<out ViewModel> = TimelineViewModel::class
    }

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun bindTimelineViewModel(viewModel: TimelineViewModel): ViewModel
}

@Module
interface UserListViewModelModule {
    companion object {
        @Provides
        fun provideUserListViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): UserListViewModel {
            val o = owner as ListOwner<QueryType.UserQueryType>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<QueryType.UserQueryType, UserListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return UserListViewModel(o, eventDispatcher, repository, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserQueryType::class)
        fun provideUserListViewModelKClass(): KClass<out ViewModel> = UserListViewModel::class
    }

    @Binds
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun bindUserListViewModel(viewModel: UserListViewModel): ViewModel
}

@Module
interface MemberListListViewModelModule {
    companion object {
        @Provides
        fun provideMemberListListViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): MemberListListViewModel {
            val o = owner as ListOwner<QueryType.UserListMembership>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<QueryType.UserListMembership, MemberListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return MemberListListViewModel(o, repository, eventDispatcher, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserListMembership::class)
        fun provideMemberListListViewModelKClass(): KClass<out ViewModel> =
            MemberListListViewModel::class
    }

    @Binds
    @IntoMap
    @ViewModelKey(MemberListListViewModel::class)
    fun bindMemberListListViewModel(viewModel: MemberListListViewModel): ViewModel
}
