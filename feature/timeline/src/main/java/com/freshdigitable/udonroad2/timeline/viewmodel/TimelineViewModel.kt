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
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.TimelineViewModelSource
import com.freshdigitable.udonroad2.timeline.TweetListItemEventListener
import com.freshdigitable.udonroad2.timeline.TweetListItemViewModel
import com.freshdigitable.udonroad2.timeline.TweetMediaEventListener
import com.freshdigitable.udonroad2.timeline.TweetMediaItemViewModel
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.merge

internal class TimelineViewModel(
    viewModelSource: TimelineViewModelSource,
    userIconViewModelSource: UserIconViewModelSource,
) : ListItemLoadableViewModel<TweetQueryType>,
    ListItemLoadableEventListener by viewModelSource,
    UserIconClickListener by userIconViewModelSource,
    TweetListItemViewModel,
    TweetListItemEventListener by viewModelSource,
    TweetMediaItemViewModel,
    TweetMediaEventListener by viewModelSource,
    ActivityEventStream by viewModelSource,
    ViewModel() {
    override val selectedItemId: LiveData<SelectedItemId?> = viewModelSource.selectedItemId
        .asLiveData(viewModelScope.coroutineContext)
    override val mediaState: LiveData<TweetMediaItemViewModel.State> = viewModelSource.mediaState
        .asLiveData(viewModelScope.coroutineContext)
    override val listState: LiveData<ListItemLoadableViewModel.State> = viewModelSource.state
        .distinctUntilChangedBy { it.isHeadingEnabled }
        .asLiveData(viewModelScope.coroutineContext)
    override val timeline: Flow<PagingData<Any>> = viewModelSource.pagedList
        .cachedIn(viewModelScope)
    override val navigationEvent: Flow<AppEffect> = merge(
        viewModelSource.navigationEvent,
        userIconViewModelSource.navEvent
    )
}
