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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.DataSource
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.timeline.ListItemLoadable

class OauthViewModel(
    dataSource: DataSource<Int, OauthItem>,
    private val eventDispatcher: EventDispatcher,
    private val viewStates: OauthViewStates,
) : ViewModel(), ListItemLoadable<QueryType.Oauth, OauthItem> {

    override val loading: LiveData<Boolean> = MutableLiveData(false)
    override val timeline: LiveData<PagedList<OauthItem>>
    val pin: LiveData<CharSequence> = viewStates.pinText
    val sendPinButtonEnabled: LiveData<Boolean> = viewStates.sendPinEnabled

    init {
        val config = PagedList.Config.Builder()
            .setMaxSize(100)
            .setPageSize(10)
            .setPrefetchDistance(10)
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(10)
            .build()
        timeline = MutableLiveData(
            PagedList.Builder(dataSource, config)
                .setNotifyExecutor {}
                .setFetchExecutor {}
                .build()
        )
    }

    override fun onRefresh() {}

    fun onLoginClicked() {
        eventDispatcher.postEvent(OauthEvent.LoginClicked)
    }

    fun onAfterPinTextChanged(pin: CharSequence) {
        eventDispatcher.postEvent(OauthEvent.PinTextChanged(pin))
    }

    fun onSendPinClicked() {
        eventDispatcher.postEvent(OauthEvent.SendPinClicked)
    }

    override fun onCleared() {
        super.onCleared()
        viewStates.clear()
    }
}

sealed class OauthEvent : AppEvent {
    object Init : OauthEvent()
    object LoginClicked : OauthEvent()
    data class PinTextChanged(val text: CharSequence) : OauthEvent()
    object SendPinClicked : OauthEvent()
    object OauthSucceeded : OauthEvent()
}
