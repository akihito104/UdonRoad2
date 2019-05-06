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

package com.freshdigitable.udonroad2.timeline

import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.TweetTimelineRepository
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import dagger.Module
import dagger.Provides

class TimelineViewModel(
    private val navigator: NavigationDispatcher,
    private val homeRepository: TweetTimelineRepository
) : TweetListItemClickListener, TweetListEventListener, ViewModel() {

    val timeline: LiveData<PagedList<TweetListItem>> by lazy {
        homeRepository.getTimeline("home", ListQuery.Timeline())
    }

    val loading: LiveData<Boolean>
        get() = homeRepository.loading

    fun onRefresh() {
        homeRepository.loadAtFront()
    }

    override fun onCleared() {
        super.onCleared()
        homeRepository.clear()
    }

    fun onFabMenuSelected(item: MenuItem) {
        Log.d("TimelineViewModel", "onFabSelected: $item")
        val selected = requireNotNull(selectedItemId.get()) { "selectedItem should not be null." }
        when (item.itemId) {
            R.id.iffabMenu_main_detail -> {
                navigator.postEvent(TimelineEvent.TweetDetailRequested(
                    selected.quoteId ?: selected.originalId))
            }
        }
    }

    override val selectedItemId: ObservableField<SelectedItemId?> = ObservableField()
    val isFabVisible: ObservableInt = ObservableInt(View.INVISIBLE)

    private fun updateSelectedItem(selected: SelectedItemId) {
        when (selected) {
            selectedItemId.get() -> selectedItemId.set(null)
            else -> selectedItemId.set(selected)
        }
        isFabVisible.set(if (selectedItemId.get() != null) View.VISIBLE else View.INVISIBLE)
    }

    override fun onBodyItemClicked(item: TweetListItem) {
        Log.d("TimelineViewModel", "onBodyItemClicked: ${item.body.id}")
        updateSelectedItem(SelectedItemId(item.originalId))
    }

    override fun onQuoteItemClicked(item: TweetListItem) {
        Log.d("TimelineViewModel", "onQuoteItemClicked: ${item.quoted?.id}")
        updateSelectedItem(SelectedItemId(item.originalId, item.quoted?.id))
    }

    override fun onUserIconClicked(item: TweetListItem) {
        navigator.postEvent(TimelineEvent.UserIconClicked(item.body.user.id))
    }
}

@Module
object TimelineViewModelModule {
    @Provides
    @JvmStatic
    fun provideTimelineViewModel(
        navigator: NavigationDispatcher,
        repositories: RepositoryComponent.Builder
    ): TimelineViewModel {
        return TimelineViewModel(navigator, repositories.build().tweetTimelineRepository())
    }
}
