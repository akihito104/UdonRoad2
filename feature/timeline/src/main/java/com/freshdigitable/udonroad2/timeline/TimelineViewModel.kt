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
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
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
) : ListItemLoadable<TweetListItem>, TweetListItemClickListener, TweetListEventListener, ViewModel() {

    private val listOwner = MutableLiveData<ListOwner>()

    val timeline: LiveData<PagedList<TweetListItem>> = Transformations.switchMap(listOwner) {
        homeRepository.getList("${it.id}", it.query)
    }

    override fun getList(listOwner: ListOwner): LiveData<PagedList<TweetListItem>> {
        this.listOwner.postValue(listOwner)
        return timeline
    }

    override val loading: LiveData<Boolean>
        get() = homeRepository.loading

    override fun onRefresh() {
        homeRepository.loadAtFront()
    }

    override fun onCleared() {
        super.onCleared()
        homeRepository.clear()
    }

    override val selectedItemId: ObservableField<SelectedItemId?> = ObservableField()

    private fun updateSelectedItem(selected: SelectedItemId) {
        when (selected) {
            selectedItemId.get() -> selectedItemId.set(null)
            else -> selectedItemId.set(selected)
        }
        navigator.postEvent(TimelineEvent.TweetItemSelected(selectedItemId.get()))
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
        navigator.postEvent(TimelineEvent.UserIconClicked(item.body.user))
    }
}

data class ListOwner(
    val id: Int,
    val query: ListQuery
)

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
