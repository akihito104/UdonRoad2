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

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class TweetInputViewModel @Inject constructor(
    private val eventDispatcher: EventDispatcher,
    private val viewState: TweetInputViewState,
) : ViewModel() {

    val isExpanded: LiveData<Boolean> = viewState.isExpanded
    val text: LiveData<String> = viewState.text
    val menuItem: LiveData<InputMenuItem> = viewState.menuItem
    val inputTask: LiveData<InputTaskState> = viewState.taskState
    val expandAnimationEvent: Flow<TweetInputEvent.Opened> =
        eventDispatcher.toAction<TweetInputEvent.Opened>().asFlow()

    fun onWriteClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Open)
    }

    fun onTweetTextChanged(text: Editable) {
        eventDispatcher.postEvent(TweetInputEvent.TextUpdated(text.toString()))
    }

    fun onSendClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Send(checkNotNull(text.value)))
    }

    fun onCancelClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Cancel)
    }

    fun onExpandAnimationEnd() {
        eventDispatcher.postEvent(TweetInputEvent.Opened)
    }

    override fun onCleared() {
        super.onCleared()
        viewState.clear()
    }
}

class TweetInputActions @Inject constructor(
    eventDispatcher: EventDispatcher
) {
    internal val openInput: AppAction<TweetInputEvent.Open> = eventDispatcher.toAction()
    internal val sendTweet: AppAction<TweetInputEvent.Send> = eventDispatcher.toAction()
    internal val cancelInput: AppAction<TweetInputEvent.Cancel> = eventDispatcher.toAction()
    internal val updateText: AppAction<TweetInputEvent.TextUpdated> = eventDispatcher.toAction()
}

enum class InputTaskState(val isExpanded: Boolean) {
    IDLING(false),

    OPENED(true),

    SENDING(false),
    FAILED(false),

    SUCCEEDED(false),
    CANCELED(false)
}

class TweetInputViewState @Inject constructor(
    collapsible: Boolean,
    actions: TweetInputActions,
    private val repository: TweetInputRepository,
    executor: AppExecutor,
) {
    private val stateEventOnSend = PublishSubject.create<InputTaskState>()
    private val _taskState: AppAction<InputTaskState> = AppAction.merge(
        AppAction.just(collapsible).map {
            if (it) InputTaskState.IDLING else InputTaskState.OPENED
        },
        actions.openInput.map { InputTaskState.OPENED },
        stateEventOnSend,
        actions.cancelInput.flatMap {
            AppAction.just(
                InputTaskState.CANCELED,
                if (collapsible) InputTaskState.IDLING else InputTaskState.OPENED
            )
        }
    )
    internal val taskState: AppViewState<InputTaskState> = _taskState.toViewState()

    internal val isExpanded: AppViewState<Boolean> = taskState.map {
        it.isExpanded
    }.distinctUntilChanged()

    private val _text: AppAction<String> = AppAction.merge(
        AppAction.just(""),
        actions.updateText.map { it.text },
        _taskState.filter { it == InputTaskState.SUCCEEDED || it == InputTaskState.CANCELED }
            .map { "" }
    )
    internal val text: AppViewState<String> = _text.toViewState()

    internal val menuItem: AppViewState<InputMenuItem> = AppAction.combineLatest(
        _taskState,
        _text.map { it.isNotBlank() }.distinctUntilChanged()
    ) { state, contentAvailable ->
        when (state) {
            InputTaskState.IDLING -> InputMenuItem.WRITE_ENABLED
            InputTaskState.OPENED -> {
                if (contentAvailable) InputMenuItem.SEND_ENABLED else InputMenuItem.SEND_DISABLED
            }
            InputTaskState.SENDING -> InputMenuItem.SEND_DISABLED
            InputTaskState.FAILED -> InputMenuItem.RETRY_ENABLED
            InputTaskState.SUCCEEDED,
            InputTaskState.CANCELED -> InputMenuItem.SEND_DISABLED
        }
    }.toViewState()

    private val disposable = CompositeDisposable(
        actions.sendTweet.suspendMap(executor.dispatcher.mainContext) {
            stateEventOnSend.onNext(InputTaskState.SENDING)
            repository.post(it.text)
        }.subscribe {
            when {
                it.isSuccess -> {
                    stateEventOnSend.onNext(InputTaskState.SUCCEEDED)
                    stateEventOnSend.onNext(
                        if (collapsible) InputTaskState.IDLING else InputTaskState.OPENED
                    )
                }
                it.exception is AppTwitterException -> {
                    stateEventOnSend.onNext(InputTaskState.FAILED)
                }
            }
        }
    )

    internal fun clear() {
        disposable.clear()
    }
}

sealed class TweetInputEvent : AppEvent {
    object Open : TweetInputEvent()
    object Opened : TweetInputEvent()
    data class Send(val text: String) : TweetInputEvent()
    object Cancel : TweetInputEvent()

    data class TextUpdated(val text: String) : TweetInputEvent()
}