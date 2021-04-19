/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.oauth

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class OauthViewModel(
    private val viewModelSource: OauthViewModelSource,
) : OauthEventListener by viewModelSource,
    ListItemLoadableViewModel<QueryType.Oauth>,
    ListItemLoadableEventListener by viewModelSource,
    ActivityEventStream by viewModelSource,
    ViewModel() {
    val state = viewModelSource.state.asLiveData(viewModelScope.coroutineContext)
    override val listState: LiveData<ListItemLoadableViewModel.State> = state.map { it }
    override val timeline: Flow<PagingData<Any>> = viewModelSource.pagedList
        .cachedIn(viewModelScope)
    val sendPinButtonEnabled: LiveData<Boolean> = state.map { it.sendPinEnabled }
        .distinctUntilChanged()

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            viewModelSource.clear()
        }
    }
}

internal interface OauthEventListener {
    val authApp: AppEventListener
    val inputPin: AppEventListener1<CharSequence>
    val sendPin: AppEventListener
}

sealed class OauthEvent : AppEvent {
    object LoginClicked : OauthEvent()
    data class PinTextChanged(val text: CharSequence) : OauthEvent()
    object SendPinClicked : OauthEvent()

    sealed class Navigation : AppEffect.Navigation {
        data class LaunchTwitter(val url: String) : Navigation()
    }
}
