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
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.impl.di.ListRepositoryComponent
import com.freshdigitable.udonroad2.data.impl.di.ListRepositoryComponentModule
import com.freshdigitable.udonroad2.data.impl.di.listRepository
import com.freshdigitable.udonroad2.data.impl.di.pagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.IntoFactory
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.UserListItem
import com.freshdigitable.udonroad2.timeline.TimelineActions
import com.freshdigitable.udonroad2.timeline.TimelineNavigationDelegate
import com.freshdigitable.udonroad2.timeline.TimelineViewState
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module(
    includes = [
        TimelineViewModelModule::class,
        UserListViewModelModule::class,
        MemberListListViewModelModule::class,
        ListRepositoryComponentModule::class
    ]
)
interface TimelineViewModelModules

@Module
internal interface TimelineViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(TimelineViewModel::class)
        @IntoFactory
        fun provideTimelineViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            viewStates: TimelineViewState,
            listRepositoryFactory: ListRepositoryComponent.Factory
        ): ViewModel = provideViewModel<QueryType.TweetQueryType, TweetListItem>(
            owner,
            listRepositoryFactory
        ) { o, repository, pagedListProvider ->
            TimelineViewModel(o, eventDispatcher, viewStates, repository, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.TweetQueryType::class)
        fun provideTimelineViewModelKClass(): KClass<out ViewModel> = TimelineViewModel::class

        @Provides
        fun provideTimelineViewState(
            owner: ListOwner<*>,
            actions: TimelineActions,
            selectedItemRepository: SelectedItemRepository,
            tweetRepository: TweetRepository,
            listOwnerGenerator: ListOwnerGenerator,
            navDelegate: TimelineNavigationDelegate
        ): TimelineViewState {
            return TimelineViewState(
                owner,
                actions,
                selectedItemRepository,
                tweetRepository,
                listOwnerGenerator,
                navDelegate
            )
        }

        @Provides
        fun provideTimelineNavigationDelegate(
            fragment: ListItemFragment,
            activityEventDelegate: ActivityEventDelegate
        ): TimelineNavigationDelegate {
            return TimelineNavigationDelegate(fragment, activityEventDelegate)
        }

        @Provides
        fun provideTimelineActions(dispatcher: EventDispatcher): TimelineActions {
            return TimelineActions(dispatcher)
        }
    }
}

@Module
internal interface UserListViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(UserListViewModel::class)
        @IntoFactory
        fun provideUserListViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            listRepositoryFactory: ListRepositoryComponent.Factory
        ): ViewModel = provideViewModel<QueryType.UserQueryType, UserListItem>(
            owner,
            listRepositoryFactory
        ) { o, repository, pagedListProvider ->
            UserListViewModel(o, eventDispatcher, repository, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserQueryType::class)
        fun provideUserListViewModelKClass(): KClass<out ViewModel> = UserListViewModel::class
    }
}

@Module
internal interface MemberListListViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(MemberListListViewModel::class)
        @IntoFactory
        fun provideMemberListListViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            listRepositoryFactory: ListRepositoryComponent.Factory
        ): ViewModel = provideViewModel<QueryType.UserListMembership, MemberListItem>(
            owner,
            listRepositoryFactory
        ) { o, repository, pagedListProvider ->
            MemberListListViewModel(o, repository, eventDispatcher, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserListMembership::class)
        fun provideMemberListListViewModelKClass(): KClass<out ViewModel> =
            MemberListListViewModel::class
    }
}

private inline fun <Q : QueryType, I> provideViewModel(
    owner: ListOwner<*>,
    listRepositoryFactory: ListRepositoryComponent.Factory,
    block: (ListOwner<Q>, ListRepository<Q>, PagedListProvider<Q, I>) -> ViewModel
): ViewModel {
    val component = listRepositoryFactory.create(owner.query)
    val listRepository = component.listRepository<Q>()
    return block(
        owner as ListOwner<Q>,
        listRepository,
        component.pagedListProvider(listRepository)
    )
}
