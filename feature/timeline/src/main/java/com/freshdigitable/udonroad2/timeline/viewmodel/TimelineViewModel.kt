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

package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.LocalListDataSourceProvider
import com.freshdigitable.udonroad2.data.db.PagedListDataSourceFactoryProvider
import com.freshdigitable.udonroad2.data.impl.AppExecutor
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.data.restclient.RemoteListDataSourceProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.Tweet
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListEventListener
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import timber.log.Timber
import kotlin.reflect.KClass

class TimelineViewModel(
    private val owner: ListOwner<TweetQueryType>,
    private val navigator: NavigationDispatcher,
    viewStateModel: FragmentContainerViewStateModel,
    private val homeRepository: ListRepository<TweetQueryType>,
    pagedListProvider: PagedListProvider<TweetQueryType, TweetListItem>
) : ListItemLoadable<TweetQueryType, TweetListItem>,
    TweetListItemClickListener,
    TweetListEventListener, ViewModel() {

    override val timeline: LiveData<PagedList<TweetListItem>> =
        pagedListProvider.getList(owner.query, owner.value) { i ->
            PageOption.OnTail(i.originalId - 1)
        }

    override val loading: LiveData<Boolean> = homeRepository.loading

    override fun onRefresh() {
        val items = timeline.value
        val query = if (items?.isNotEmpty() == true) {
            ListQuery(owner.query, PageOption.OnHead(items.first().originalId + 1))
        } else {
            ListQuery(owner.query, PageOption.OnInit)
        }
        homeRepository.loadList(query, owner.value)
    }

    override fun onCleared() {
        Timber.tag("TimelineViewModel").d("onCleared: $owner")
        super.onCleared()
        homeRepository.clear(owner.value)
    }

    override val selectedItemId: LiveData<SelectedItemId?> = viewStateModel.selectedItemId

    override fun onBodyItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.body.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId))
    }

    override fun onQuoteItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onQuoteItemClicked: ${item.quoted?.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId, item.quoted?.id))
    }

    private fun updateSelectedItem(selected: SelectedItemId) {
        navigator.postEvent(TimelineEvent.ToggleTweetItemSelectedState(selected))
    }

    override fun onUserIconClicked(user: TweetingUser) {
        navigator.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onMediaItemClicked(originalId: Long, item: Tweet, index: Int) {
        navigator.postEvent(TimelineEvent.MediaItemClicked(item.id, index))
    }
}

interface FragmentContainerViewStateModel {
    val selectedItemId: LiveData<SelectedItemId?>
}

@Module
interface TimelineViewModelModule {
    companion object {
        @Provides
        fun provideTimelineViewModel(
            owner: ListOwner<*>,
            navigator: NavigationDispatcher,
            viewStateModel: FragmentContainerViewStateModel,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): TimelineViewModel {
            val o = owner as ListOwner<TweetQueryType>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<TweetQueryType, TweetListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return TimelineViewModel(o, navigator, viewStateModel, repository, pagedListProvider)
        }

        @Provides
        @IntoMap
        @QueryTypeKey(TweetQueryType::class)
        fun provideTimelineViewModelKClass(): KClass<out ViewModel> = TimelineViewModel::class
    }

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun bindTimelineViewModel(viewModel: TimelineViewModel): ViewModel
}
