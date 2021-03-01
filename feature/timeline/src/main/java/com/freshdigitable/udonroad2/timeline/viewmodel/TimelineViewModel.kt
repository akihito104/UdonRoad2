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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineViewState
import com.freshdigitable.udonroad2.timeline.TweetListItemViewModel
import com.freshdigitable.udonroad2.timeline.TweetMediaItemViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import timber.log.Timber

internal class TimelineViewModel(
    private val owner: ListOwner<TweetQueryType>,
    private val eventDispatcher: EventDispatcher,
    viewStates: TimelineViewState,
) : ListItemLoadableViewModel<TweetQueryType>, ListItemLoadableEventListener by viewStates,
    TweetListItemViewModel, TweetMediaItemViewModel, ActivityEventStream by viewStates,
    ViewModel() {
    private val state = viewStates.state.shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)
    override val selectedItemId: LiveData<SelectedItemId?> = viewStates.selectedItemId
    override val mediaState: LiveData<TweetMediaItemViewModel.State> =
        state.distinctUntilChangedBy { it.mediaState }
            .filterNotNull()
            .asLiveData(viewModelScope.coroutineContext)
    override val listState: LiveData<ListItemLoadableViewModel.State> =
        state.distinctUntilChangedBy { it.isHeadingEnabled }
            .asLiveData(viewModelScope.coroutineContext)
    override val timeline: Flow<PagingData<Any>> = viewStates.pagedList.cachedIn(viewModelScope)

    override fun onBodyItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.body.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId))
    }

    override fun onQuoteItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onQuoteItemClicked: ${item.quoted?.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId, item.quoted?.id))
    }

    private fun updateSelectedItem(selected: SelectedItemId) {
        eventDispatcher.postEvent(TimelineEvent.TweetItemSelection.Toggle(selected))
    }

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onMediaItemClicked(
        originalId: TweetId,
        quotedId: TweetId?,
        item: TweetElement,
        index: Int
    ) {
        val selected = SelectedItemId(owner, originalId, quotedId)
        eventDispatcher.postEvent(TimelineEvent.MediaItemClicked(item.id, index, selected))
    }
}
