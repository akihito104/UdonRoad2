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
import androidx.paging.DataSource
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

class OauthViewModel(
    dataSource: DataSource<Int, OauthItem>,
    private val eventDispatcher: EventDispatcher,
    viewStates: OauthViewStates,
) : ListItemLoadableViewModel<QueryType.Oauth, OauthItem>() {

    override val loading: LiveData<Boolean> = MutableLiveData(false)
    override val timeline: LiveData<PagedList<OauthItem>>
    val pin: LiveData<CharSequence> = viewStates.pinText
    val sendPinButtonEnabled: LiveData<Boolean> = viewStates.sendPinEnabled
    override val navigationEvent: Flow<NavigationEvent> =
        merge(viewStates.launchTwitterOauth, viewStates.completeAuthProcess)
    override val feedbackMessage: Flow<FeedbackMessage> = emptyFlow()

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
}

sealed class OauthEvent : AppEvent {
    object Init : OauthEvent()
    object LoginClicked : OauthEvent()
    data class PinTextChanged(val text: CharSequence) : OauthEvent()
    object SendPinClicked : OauthEvent()

    sealed class Navigation : NavigationEvent {
        data class LaunchTwitter(val url: String) : Navigation()
    }
}
