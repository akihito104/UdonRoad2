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
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.DispatcherProvider
import com.freshdigitable.udonroad2.model.app.ext.merge
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventResult
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.withContext

class OauthViewStates(
    actions: OauthAction,
    private val navDelegate: OauthNavigationDelegate,
    repository: OAuthTokenRepository,
    savedState: OauthSavedStates,
    appExecutor: AppExecutor,
) {
    private val _requestToken: AppAction<EventResult<OauthEvent.LoginClicked, RequestTokenItem>> =
        actions.authApp.suspendMap(appExecutor.dispatcher.mainContext) {
            val token = repository.getRequestTokenItem()
            savedState.setToken(token)
            token
        }
    private val requestToken: LiveData<RequestTokenItem?> = savedState.requestTokenItem

    internal val pinText: LiveData<CharSequence> = actions.inputPin.map {
        it.text
    }.toViewState()

    internal val sendPinEnabled: AppViewState<Boolean> = merge(requestToken, pinText) { t, p ->
        t != null && p?.isNotEmpty() == true
    }

    private val completeAuthProcess: AppAction<EventResult<OauthEvent.SendPinClicked, AccessTokenEntity>> =
        actions.sendPin.suspendMap(appExecutor.dispatcher.mainContext) {
            val token = requireNotNull(requestToken.value)
            val verifier = pinText.value.toString()
            val t = repository.getAccessToken(token, verifier)
            savedState.setToken(null)
            repository.login(t.userId)
            t
        }

    private val disposables = CompositeDisposable(
        _requestToken.subscribe {
            when {
                it.isSuccess -> navDelegate.launchTwitterOauth(requireNotNull(it.value).authorizationUrl)
                else -> it.rethrow() // FIXME: send feedback
            }
        },
        completeAuthProcess.subscribe {
            navDelegate.toTimeline()
        },
    )

    fun clear() {
        disposables.clear()
        navDelegate.clear()
    }
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
