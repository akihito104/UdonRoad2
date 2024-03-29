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
import com.freshdigitable.udonroad2.timeline.LaunchMediaViewerAction
import com.freshdigitable.udonroad2.timeline.ListItemLoadableActions
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewStateImpl
import com.freshdigitable.udonroad2.timeline.TimelineActions
import com.freshdigitable.udonroad2.timeline.TimelineViewModelSource
import com.freshdigitable.udonroad2.timeline.TweetMediaViewModelSource
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
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
            actions: ListItemLoadableActions,
            listRepositoryFactory: ListRepositoryComponent.Factory,
            appExecutor: AppExecutor,
        ): ListItemLoadableViewStateImpl =
            provideViewState<QueryType, ListItemLoadableViewStateImpl>(
                owner,
                listRepositoryFactory
            ) { o, repository, pagedListProvider ->
                ListItemLoadableViewStateImpl(
                    o,
                    actions,
                    repository,
                    pagedListProvider,
                    appExecutor,
                )
            }
    }
}

@Module
internal interface TweetMediaViewModelModule {
    companion object {
        @Provides
        fun provideTweetMediaViewModelSource(
            actions: LaunchMediaViewerAction,
            appSettingRepository: AppSettingRepository,
        ): TweetMediaViewModelSource =
            TweetMediaViewModelSource.create(actions, appSettingRepository)
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
            viewStates: TimelineViewModelSource,
            userIconViewModelSource: UserIconViewModelSource,
        ): ViewModel = TimelineViewModel(viewStates, userIconViewModelSource)

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.Tweet::class)
        fun provideTimelineViewModelKClass(): KClass<out ViewModel> = TimelineViewModel::class

        @Provides
        fun provideTimelineViewModelSource(
            owner: ListOwner<*>,
            actions: TimelineActions,
            selectedItemRepository: SelectedItemRepository,
            appSettingRepository: AppSettingRepository,
            viewModelSource: ListItemLoadableViewModelSource,
        ): TimelineViewModelSource = TimelineViewModelSource(
            owner as ListOwner<QueryType.Tweet>,
            actions,
            selectedItemRepository,
            viewModelSource,
            TweetMediaViewModelSource.create(actions, appSettingRepository),
        )

        @Provides
        fun provideListItemLoadableViewModelSource(
            owner: ListOwner<*>,
            actions: TimelineActions,
            listRepositoryFactory: ListRepositoryComponent.Factory,
            appExecutor: AppExecutor,
        ): ListItemLoadableViewModelSource =
            provideViewState<QueryType, ListItemLoadableViewModelSource>(
                owner,
                listRepositoryFactory
            ) { o, repository, pagedListProvider ->
                ListItemLoadableViewStateImpl(
                    o,
                    actions,
                    repository,
                    pagedListProvider,
                    appExecutor,
                )
            }

        @Provides
        fun provideTimelineActions(
            owner: ListOwner<*>,
            dispatcher: EventDispatcher,
            listItemLoadableActions: ListItemLoadableActions,
            mediaViewerAction: LaunchMediaViewerAction,
        ): TimelineActions =
            TimelineActions(owner, dispatcher, listItemLoadableActions, mediaViewerAction)
    }
}

@Module(includes = [ListItemLoadableViewModelSourceModule::class])
internal interface UserListViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(UserListViewModel::class)
        @IntoFactory
        fun provideUserListViewModel(
            viewModelSource: ListItemLoadableViewStateImpl,
            userIconViewModelSource: UserIconViewModelSource,
        ): ViewModel = UserListViewModel(viewModelSource, userIconViewModelSource)

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.User::class)
        fun provideUserListViewModelKClass(): KClass<out ViewModel> = UserListViewModel::class
    }
}

@Module(includes = [ListItemLoadableViewModelSourceModule::class])
internal interface CustomTimelineListViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(CustomTimelineListViewModel::class)
        @IntoFactory
        fun provideCustomTimelineListViewModel(
            actions: CustomTimelineListActions,
            viewModelSource: ListItemLoadableViewStateImpl,
            listOwnerGenerator: ListOwnerGenerator,
            userIconViewModelSource: UserIconViewModelSource,
        ): ViewModel = CustomTimelineListViewModel(
            CustomTimelineListItemLoadableViewState(
                actions,
                viewModelSource,
                listOwnerGenerator,
            ),
            userIconViewModelSource
        )

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.CustomTimelineList::class)
        fun provideCustomTimelineListViewModelKClass(): KClass<out ViewModel> =
            CustomTimelineListViewModel::class
    }
}

private inline fun <Q : QueryType, VS : ListItemLoadableViewModelSource> provideViewState(
    owner: ListOwner<*>,
    listRepositoryFactory: ListRepositoryComponent.Factory,
    block: (ListOwner<Q>, ListRepository<Q, Any>, PagedListProvider<Q, Any>) -> VS,
): VS {
    val component = listRepositoryFactory.create(owner.query)
    val listRepository = component.listRepository<Q>()
    return block(
        owner as ListOwner<Q>,
        listRepository,
        component.pagedListProvider(listRepository)
    )
}
