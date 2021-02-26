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
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.impl.di.ListRepositoryComponent
import com.freshdigitable.udonroad2.data.impl.di.ListRepositoryComponentModule
import com.freshdigitable.udonroad2.data.impl.di.listRepository
import com.freshdigitable.udonroad2.data.impl.di.pagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.di.IntoFactory
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemLoadableActionsImpl
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewState
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewStateImpl
import com.freshdigitable.udonroad2.timeline.TimelineActions
import com.freshdigitable.udonroad2.timeline.TimelineViewState
import com.freshdigitable.udonroad2.timeline.viewmodel.CustomTimelineListActions
import com.freshdigitable.udonroad2.timeline.viewmodel.CustomTimelineListItemLoadableViewState
import com.freshdigitable.udonroad2.timeline.viewmodel.CustomTimelineListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module(
    includes = [
        ListItemLoadableViewModelSourceModule::class,
        TimelineViewModelModule::class,
        UserListViewModelModule::class,
        CustomTimelineListViewModelModule::class,
        ListRepositoryComponentModule::class,
    ]
)
interface TimelineViewModelModules

@Module
internal interface ListItemLoadableViewModelSourceModule {
    companion object {
        @Provides
        fun provideListItemLoadableViewState(
            owner: ListOwner<*>,
            actions: ListItemLoadableActionsImpl,
            listRepositoryFactory: ListRepositoryComponent.Factory,
        ): ListItemLoadableViewState = provideViewState<QueryType, ListItemLoadableViewStateImpl>(
            owner,
            listRepositoryFactory
        ) { o, repository, pagedListProvider ->
            ListItemLoadableViewStateImpl(
                o,
                actions,
                repository,
                pagedListProvider
            )
        }
    }
}

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
        ): ViewModel = TimelineViewModel(
            owner as ListOwner<QueryType.TweetQueryType>,
            eventDispatcher,
            viewStates
        )

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
            appSettingRepository: AppSettingRepository,
            listOwnerGenerator: ListOwnerGenerator,
            executor: AppExecutor,
            viewState: ListItemLoadableViewState,
        ): TimelineViewState = TimelineViewState(
            owner as ListOwner<QueryType.TweetQueryType>,
            actions,
            selectedItemRepository,
            tweetRepository,
            appSettingRepository,
            listOwnerGenerator,
            executor,
            viewState,
        )

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
            viewState: ListItemLoadableViewState,
        ): ViewModel = UserListViewModel(
            owner as ListOwner<QueryType.UserQueryType>,
            eventDispatcher,
            viewState
        )

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.UserQueryType::class)
        fun provideUserListViewModelKClass(): KClass<out ViewModel> = UserListViewModel::class
    }
}

@Module
internal interface CustomTimelineListViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(CustomTimelineListViewModel::class)
        @IntoFactory
        fun provideCustomTimelineListViewModel(
            owner: ListOwner<*>,
            eventDispatcher: EventDispatcher,
            actions: CustomTimelineListActions,
            viewState: ListItemLoadableViewState,
            listOwnerGenerator: ListOwnerGenerator,
        ): ViewModel = CustomTimelineListViewModel(
            owner as ListOwner<QueryType.CustomTimelineListQueryType>,
            CustomTimelineListItemLoadableViewState(actions, viewState, listOwnerGenerator),
            eventDispatcher,
        )

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.CustomTimelineListQueryType::class)
        fun provideCustomTimelineListViewModelKClass(): KClass<out ViewModel> =
            CustomTimelineListViewModel::class
    }
}

private inline fun <Q : QueryType, VS : ListItemLoadableViewState> provideViewState(
    owner: ListOwner<*>,
    listRepositoryFactory: ListRepositoryComponent.Factory,
    block: (ListOwner<Q>, ListRepository<Q, Any>, PagedListProvider<Q, Any>) -> VS
): VS {
    val component = listRepositoryFactory.create(owner.query)
    val listRepository = component.listRepository<Q>()
    return block(
        owner as ListOwner<Q>,
        listRepository,
        component.pagedListProvider(listRepository)
    )
}
