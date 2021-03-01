/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.app.DispatcherProvider
import com.freshdigitable.udonroad2.model.app.ext.combineLatest
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext

class OauthViewStates(
    actions: OauthAction,
    login: LoginUseCase,
    dataSource: PagingSource<Int, OauthItem>,
    repository: OAuthTokenDataSource,
    listOwnerGenerator: ListOwnerGenerator,
    savedState: OauthSavedStates,
) : ListItemLoadableViewModelSource, ActivityEventStream {
    private val launchTwitterOauth: Flow<NavigationEvent> = actions.authApp.asFlow().mapLatest {
        val token = repository.getRequestTokenItem()
        savedState.setToken(token)
        OauthEvent.Navigation.LaunchTwitter(token.authorizationUrl)
    }
    private val requestToken: LiveData<RequestTokenItem?> = savedState.requestTokenItem

    internal val pinText: LiveData<CharSequence> = actions.inputPin.map {
        it.text
    }.toViewState()

    internal val sendPinEnabled: AppViewState<Boolean> =
        combineLatest(requestToken, pinText) { t, p ->
            t != null && p?.isNotEmpty() == true
        }

    private val completeAuthProcess: Flow<NavigationEvent> = actions.sendPin.asFlow().mapLatest {
        val token = requireNotNull(requestToken.value)
        val verifier = pinText.value.toString()
        val t = repository.getAccessToken(token, verifier)
        savedState.setToken(null)
        login(t.userId)
        listOwnerGenerator.getTimelineEvent(
            QueryType.TweetQueryType.Timeline(), NavigationEvent.Type.INIT
        )
    }
    override val pagedList: Flow<PagingData<Any>> = Pager(
        config = PagingConfig(
            maxSize = 100,
            pageSize = 10,
            prefetchDistance = 10,
            enablePlaceholders = false,
            initialLoadSize = 10
        ),
        pagingSourceFactory = { dataSource }
    ).flow as Flow<PagingData<Any>>

    override val state: Flow<ListItemLoadableViewModel.State> =
        flowOf(object : ListItemLoadableViewModel.State {
            override val isHeadingEnabled: Boolean = false
        })
    override val navigationEvent: Flow<NavigationEvent> =
        merge(launchTwitterOauth, completeAuthProcess)
    override val feedbackMessage: Flow<FeedbackMessage> = emptyFlow()
    override fun onRefresh() = Unit
    override fun onListScrollStarted() = Unit
    override fun onListScrollStopped(firstVisibleItemPosition: Int) = Unit
    override fun onHeadingClicked() = Unit
}

class OauthSavedStates(
    handle: SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider = DispatcherProvider()
) {
    private val _requestTokenItem: MutableLiveData<RequestTokenItem?> = handle.getLiveData(
        SAVED_STATE_REQUEST_TOKEN
    )
    internal val requestTokenItem: LiveData<RequestTokenItem?> = _requestTokenItem

    internal suspend fun setToken(token: RequestTokenItem?) =
        withContext(dispatcherProvider.mainContext) {
            _requestTokenItem.value = token
        }

    companion object {
        private const val SAVED_STATE_REQUEST_TOKEN = "saveState_requestToken"
    }
}
