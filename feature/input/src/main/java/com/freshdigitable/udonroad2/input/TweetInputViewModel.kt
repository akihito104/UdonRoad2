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

package com.freshdigitable.udonroad2.input

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.ext.merge
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.suspendCreate
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class TweetInputViewModel @Inject constructor(
    private val eventDispatcher: EventDispatcher,
    private val viewState: TweetInputViewState,
) : ViewModel() {

    val isVisible: LiveData<Boolean> = viewState.isVisible
    val text: LiveData<String> = viewState.text
    val menuItem: LiveData<InputMenuItem> = viewState.menuItem

    fun onWriteClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Open)
    }

    fun onTweetTextChanged(text: String) {
        eventDispatcher.postEvent(TweetInputEvent.TextUpdated(text))
    }

    fun onSendClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Send)
    }

    fun onCloseClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Close)
    }

    override fun onCleared() {
        super.onCleared()
        viewState.clear()
    }
}

class TweetInputActions @Inject constructor(
    eventDispatcher: EventDispatcher
) {
    val openInput: AppAction<TweetInputEvent.Open> = eventDispatcher.toAction()
    val sendTweet: AppAction<TweetInputEvent.Send> = eventDispatcher.toAction()
    val closeInput: AppAction<TweetInputEvent.Close> = eventDispatcher.toAction()
    val updateText: AppAction<TweetInputEvent.TextUpdated> = eventDispatcher.toAction()
}

class TweetInputViewState @Inject constructor(
    collapsible: Boolean,
    actions: TweetInputActions,
    private val repository: TweetInputRepository,
    executor: AppExecutor,
) {
    private val _state: AppViewState<TweetInputState> = AppAction.merge(
        AppAction.just(collapsible).map {
            if (it) TweetInputState.IDLING else TweetInputState.OPENED
        },
        actions.openInput.map { TweetInputState.OPENED },
        actions.sendTweet.suspendCreate<TweetInputEvent.Send, TweetInputState>(
            executor.dispatcher.mainContext
        ) {
            send(Result.success(TweetInputState.SENDING))
            try {
                repository.post()
                send(Result.success(TweetInputState.SUCCEEDED))
                repository.clear()
                send(Result.success(TweetInputState.IDLING))
            } catch (e: AppTwitterException) {
                send(Result.success(TweetInputState.FAILED))
            } catch (t: Throwable) {
                send(Result.failure(t))
            }
        }.map {
            when (it.isSuccess) {
                true -> it.value
                else -> it.rethrow()
            }
        },
        actions.closeInput.map {
            repository.clear()
            TweetInputState.IDLING
        },
    ).toViewState()
    val isVisible: LiveData<Boolean> = _state.map {
        when (it) {
            TweetInputState.OPENED -> true
            else -> false
        }
    }

    val text: LiveData<String> = repository.text.asLiveData(executor.dispatcher.mainContext)
    val menuItem: LiveData<InputMenuItem> = merge(_state, text) { s, t ->
        when (s) {
            null -> throw IllegalStateException()
            TweetInputState.OPENED -> {
                if (t.isNullOrBlank()) {
                    InputMenuItem.SEND_DISABLED
                } else {
                    InputMenuItem.SEND_ENABLED
                }
            }
            else -> s.toMenuItem()
        }
    }
    private val disposable = CompositeDisposable(
        actions.updateText.subscribe { repository.updateText(it.text) }
    )

    fun clear() {
        disposable.clear()
    }
}

sealed class TweetInputEvent : AppEvent {
    object Open : TweetInputEvent()
    object Close : TweetInputEvent()
    object Send : TweetInputEvent()

    data class TextUpdated(val text: String) : TweetInputEvent()
}
