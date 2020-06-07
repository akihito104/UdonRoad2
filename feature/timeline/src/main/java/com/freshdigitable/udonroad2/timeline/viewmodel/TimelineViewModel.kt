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

import android.util.Log
import androidx.databinding.ObservableField
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
import com.freshdigitable.udonroad2.model.ListQuery.TweetListQuery
import com.freshdigitable.udonroad2.model.Tweet
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.ViewModelKey
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.SelectedItemId
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetListEventListener
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

class TimelineViewModel(
    private val owner: ListOwner<TweetListQuery>,
    private val navigator: NavigationDispatcher,
    private val homeRepository: ListRepository<TweetListQuery>,
    pagedListProvider: PagedListProvider<TweetListQuery, TweetListItem>
) : ListItemLoadable<TweetListQuery, TweetListItem>,
    TweetListItemClickListener,
    TweetListEventListener, ViewModel() {

    override val timeline: LiveData<PagedList<TweetListItem>> =
        pagedListProvider.getList(owner.query, owner.value)

    override val loading: LiveData<Boolean>
        get() = homeRepository.loading

    override fun onRefresh() {
        homeRepository.loadList(owner.query, owner.value)
    }

    override fun onCleared() {
        super.onCleared()
        homeRepository.clear(owner.value)
    }

    override val selectedItemId: ObservableField<SelectedItemId?> = ObservableField()

    private fun updateSelectedItem(selected: SelectedItemId) {
        when (selected) {
            selectedItemId.get() -> selectedItemId.set(null)
            else -> selectedItemId.set(selected)
        }
        navigator.postEvent(
            TimelineEvent.TweetItemSelected(selectedItemId.get())
        )
    }

    override fun onBodyItemClicked(item: TweetListItem) {
        Log.d("TimelineViewModel", "onBodyItemClicked: ${item.body.id}")
        updateSelectedItem(SelectedItemId(item.originalId))
    }

    override fun onQuoteItemClicked(item: TweetListItem) {
        Log.d("TimelineViewModel", "onQuoteItemClicked: ${item.quoted?.id}")
        updateSelectedItem(
            SelectedItemId(item.originalId, item.quoted?.id)
        )
    }

    override fun onUserIconClicked(item: TweetListItem) {
        navigator.postEvent(TimelineEvent.UserIconClicked(item.body.user))
    }

    override fun onMediaItemClicked(originalId: Long, item: Tweet, index: Int) {
        updateSelectedItem(SelectedItemId(originalId, item.id))
        navigator.postEvent(TimelineEvent.MediaItemClicked(item.id, index))
    }
}

@Module
interface TimelineViewModelModule {
    companion object {
        @Provides
        fun provideTimelineViewModel(
            owner: ListOwner<*>,
            navigator: NavigationDispatcher,
            localListDataSourceProvider: LocalListDataSourceProvider,
            remoteListDataSourceProvider: RemoteListDataSourceProvider,
            pagedListDataSourceFactoryProvider: PagedListDataSourceFactoryProvider,
            executor: AppExecutor
        ): TimelineViewModel {
            val o = owner as ListOwner<TweetListQuery>
            val repository = ListRepository.create(
                o.query,
                localListDataSourceProvider,
                remoteListDataSourceProvider,
                executor
            )
            val pagedListProvider: PagedListProvider<TweetListQuery, TweetListItem> =
                PagedListProvider.create(
                    pagedListDataSourceFactoryProvider.get(o.query),
                    repository,
                    executor
                )
            return TimelineViewModel(o, navigator, repository, pagedListProvider)
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun bindTimelineViewModel(viewModel: TimelineViewModel): ViewModel
}
