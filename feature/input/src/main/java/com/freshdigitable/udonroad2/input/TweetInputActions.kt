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

import android.net.Uri
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import javax.inject.Inject

class TweetInputActions @Inject constructor(
    eventDispatcher: EventDispatcher
) {
    internal val openInput: AppAction<TweetInputEvent.Open> = eventDispatcher.toAction()
    internal val sendTweet: AppAction<TweetInputEvent.Send> = eventDispatcher.toAction()
    internal val cancelInput: AppAction<TweetInputEvent.Cancel> = eventDispatcher.toAction()
    internal val updateText: AppAction<TweetInputEvent.TextUpdated> = eventDispatcher.toAction()
    internal val cameraApp: AppAction<CameraApp.Event> = eventDispatcher.toAction()
}

sealed class TweetInputEvent : AppEvent {
    object Open : TweetInputEvent()
    object Opened : TweetInputEvent()
    data class Send(val text: String) : TweetInputEvent()
    object Cancel : TweetInputEvent()

    data class TextUpdated(val text: String) : TweetInputEvent()
}

sealed class CameraApp : TweetInputEvent() {
    sealed class Event : CameraApp() {
        data class CandidateQueried(val apps: List<Components>, val uri: Uri) : Event()
        data class Chosen(val app: Components) : Event()
        object OnFinish : Event()
    }

    sealed class State : CameraApp() {
        object Idling : State() {
            override fun transition(event: Event): State {
                return when (event) {
                    is Event.Chosen -> throw IllegalStateException()
                    is Event.CandidateQueried -> WaitingForChosen(event.apps, event.uri)
                    is Event.OnFinish -> this
                }
            }
        }

        data class WaitingForChosen(val apps: List<Components>, val uri: Uri) : State() {
            override fun transition(event: Event): State {
                return when (event) {
                    is Event.Chosen -> {
                        if (apps.contains(event.app)) Selected(event.app, uri) else Idling
                    }
                    is Event.CandidateQueried -> WaitingForChosen(event.apps, event.uri)
                    is Event.OnFinish -> Idling
                }
            }
        }

        data class Selected(val app: Components, val uri: Uri) : State() {
            override fun transition(event: Event): State {
                return when (event) {
                    is Event.OnFinish -> Finished(app, uri)
                    is Event.Chosen, is Event.CandidateQueried -> throw IllegalStateException()
                }
            }
        }

        data class Finished(val app: Components, val uri: Uri) : State() {
            override fun transition(event: Event): State {
                return when (event) {
                    is Event.CandidateQueried -> WaitingForChosen(event.apps, event.uri)
                    is Event.Chosen, is Event.OnFinish -> throw IllegalStateException()
                }
            }
        }

        abstract fun transition(event: Event): State
    }
}

data class Components(
    val packageName: String,
    val className: String
) {
    companion object
}
