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
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineViewState
import com.freshdigitable.udonroad2.timeline.TweetListEventListener
import com.freshdigitable.udonroad2.timeline.TweetListItemClickListener
import timber.log.Timber

class TimelineViewModel(
    private val owner: ListOwner<TweetQueryType>,
    private val eventDispatcher: EventDispatcher,
    private val viewStates: TimelineViewState,
    private val homeRepository: ListRepository<TweetQueryType>,
    pagedListProvider: PagedListProvider<TweetQueryType, TweetListItem>
) : ListItemLoadable<TweetQueryType, TweetListItem>,
    TweetListItemClickListener,
    TweetListEventListener, ViewModel() {

    override val timeline: LiveData<PagedList<TweetListItem>> =
        pagedListProvider.getList(owner.query, owner.value) { i ->
            PageOption.OnTail(i.originalId.value - 1)
        }

    override val loading: LiveData<Boolean> = homeRepository.loading

    override fun onRefresh() {
        val items = timeline.value
        val query = if (items?.isNotEmpty() == true) {
            ListQuery(owner.query, PageOption.OnHead(items.first().originalId.value + 1))
        } else {
            ListQuery(owner.query, PageOption.OnInit)
        }
        homeRepository.loadList(query, owner.value)
    }

    override fun onCleared() {
        Timber.tag("TimelineViewModel").d("onCleared: $owner")
        super.onCleared()
        homeRepository.clear(owner.value)
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
        item: Tweet,
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
