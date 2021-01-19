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
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineViewState
import com.freshdigitable.udonroad2.timeline.TweetListEventListener
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

class TimelineViewModel(
    private val owner: ListOwner<TweetQueryType>,
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TimelineViewState,
    private val homeRepository: ListRepository<TweetQueryType>,
    pagedListProvider: PagedListProvider<TweetQueryType, TweetListItem>
) : ListItemLoadableViewModel<TweetQueryType, TweetListItem>(
    eventDispatcher,
    viewStates,
    viewStates
),
    TweetListItemClickListener,
    TweetListEventListener {

    override val timeline: Flow<PagingData<TweetListItem>> =
        pagedListProvider.getList(owner.query, owner.id).cachedIn(viewModelScope)

    override fun onRefresh() {
        viewModelScope.launch {
            homeRepository.prependList(owner.query, owner.id)
        }
    }

    override fun onCleared() {
        Timber.tag("TimelineViewModel").d("onCleared: $owner")
        super.onCleared()
        viewModelScope.launch {
            homeRepository.clear(owner.id)
        }
        viewStates.clear()
    }

    override val selectedItemId: LiveData<SelectedItemId?> = viewStates.selectedItemId

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
        eventDispatcher.postEvent(
            TimelineEvent.MediaItemClicked(
                item.id,
                index,
                SelectedItemId(owner, originalId, quotedId)
            )
        )
    }
}
