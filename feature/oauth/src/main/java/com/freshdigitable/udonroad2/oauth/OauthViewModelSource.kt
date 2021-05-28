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
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.getTimelineEvent
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

internal class OauthViewModelSource(
    actions: OauthAction,
    login: LoginUseCase,
    dataSource: PagingSource<Int, OauthItem>,
    repository: OAuthTokenDataSource,
    listOwnerGenerator: ListOwnerGenerator,
    savedState: OauthSavedStates,
) : ListItemLoadableViewModelSource,
    ListItemLoadableEventListener by actions,
    OauthEventListener by actions,
    ActivityEffectStream {
    private val navigationChannel = Channel<AppEffect.Navigation>()
    override val state: Flow<OauthViewState> = stateSourceBuilder(
        init = OauthViewState(),
        actions.authApp.onEvent { s, _ ->
            val token = repository.getRequestTokenItem()
            navigationChannel.send(OauthNavigation.LaunchTwitter(token.authorizationUrl))
            s.copy(requestToken = token)
        },
        actions.inputPin.onEvent { s, e -> s.copy(pinText = e.text) },
        actions.sendPin.onEvent { s, _ ->
            val token = requireNotNull(s.requestToken)
            val t = repository.getAccessToken(token, s.pinText.toString())
            login(t.userId)
            val timelineEvent = listOwnerGenerator.getTimelineEvent(
                QueryType.Tweet.Timeline(), AppEffect.Navigation.Type.INIT
            )
            navigationChannel.send(timelineEvent)
            OauthViewState()
        }
    ).onEach {
        savedState.setToken(it.requestToken)
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

    override val effect: Flow<AppEffect.Navigation> = navigationChannel.receiveAsFlow()

    override suspend fun clear() {
        super.clear()
        navigationChannel.close()
    }
}

data class OauthViewState(
    val requestToken: RequestTokenItem? = null,
    val pinText: CharSequence = "",
) : ListItemLoadableViewModel.State {
    override val isHeadingEnabled: Boolean = false
    val sendPinEnabled: Boolean
        get() = requestToken != null && pinText.isNotEmpty()
}

class OauthSavedStates(
    handle: SavedStateHandle,
) {
    private val requestTokenItem: MutableLiveData<RequestTokenItem?> =
        handle.getLiveData(SAVED_STATE_REQUEST_TOKEN)

    internal fun setToken(token: RequestTokenItem?) {
        if (requestTokenItem.value != token) {
            requestTokenItem.postValue(token)
        }
    }

    companion object {
        private const val SAVED_STATE_REQUEST_TOKEN = "saveState_requestToken"
    }
}
