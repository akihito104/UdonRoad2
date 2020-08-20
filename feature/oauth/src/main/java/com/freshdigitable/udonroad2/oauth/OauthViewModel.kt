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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.impl.DispatcherProvider
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.app.ext.merge
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import kotlinx.coroutines.launch

class OauthViewModel(
    dataSource: DataSource<Int, OauthItem>,
    private val repository: OAuthTokenRepository,
    private val eventDispatcher: EventDispatcher,
    private val savedState: OauthSavedStates,
    private val coroutineDispatchers: DispatcherProvider = DispatcherProvider()
) : ViewModel(), ListItemLoadable<QueryType.Oauth, OauthItem> {

    override val loading: LiveData<Boolean> = MutableLiveData(false)
    override val timeline: LiveData<PagedList<OauthItem>>

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

    private val requestToken: LiveData<RequestTokenItem?> = savedState.requestTokenItem

    fun onLoginClicked() {
        viewModelScope.launch(coroutineDispatchers.mainContext) {
            repository.getRequestTokenItem().also {
                savedState.setToken(it)
                eventDispatcher.postEvent(OauthEvent.OauthRequested(it.authorizationUrl))
            }
        }
    }

    private val _pin = MutableLiveData("")
    val pin: LiveData<String> = _pin.distinctUntilChanged()

    val sendPinButtonEnabled: LiveData<Boolean> = merge(requestToken, pin) { token, p ->
        token != null && !p.isNullOrEmpty()
    }

    fun onAfterPinTextChanged(pin: CharSequence) {
        _pin.value = pin.toString()
    }

    fun onSendPinClicked() {
        viewModelScope.launch(coroutineDispatchers.mainContext) {
            val t = repository.getAccessToken(requestToken.value!!, pin.value.toString())
            repository.login(t.userId)
            savedState.setToken(null)
            eventDispatcher.postEvent(OauthEvent.OauthSucceeded)
        }
    }

}

sealed class OauthEvent : NavigationEvent {
    object Init : OauthEvent()
    data class OauthRequested(val authUrl: String) : OauthEvent()
    object OauthSucceeded : OauthEvent()
}

class OauthSavedStates(handle: SavedStateHandle) {
    private val _requestTokenItem: MutableLiveData<RequestTokenItem?> = handle.getLiveData(
        SAVED_STATE_REQUEST_TOKEN
    )
    val requestTokenItem: LiveData<RequestTokenItem?> = _requestTokenItem

    fun setToken(token: RequestTokenItem?) {
        _requestTokenItem.value = token
    }

    companion object {
        private const val SAVED_STATE_REQUEST_TOKEN = "saveState_requestToken"
    }
}
