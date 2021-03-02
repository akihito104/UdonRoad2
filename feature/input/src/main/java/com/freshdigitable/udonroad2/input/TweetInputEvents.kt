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

import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.StateGraph
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent

sealed class TweetInputEvent : AppEvent {
    object Open : TweetInputEvent()
    object Opened : TweetInputEvent()
    object Send : TweetInputEvent()
    object Cancel : TweetInputEvent()
    data class TextUpdated(val text: String) : TweetInputEvent()
}

internal sealed class CameraApp : TweetInputEvent() {
    sealed class Event : CameraApp() {
        data class CandidateQueried(val apps: List<Components>, val path: AppFilePath) : Event()
        data class Chosen(val app: Components) : Event()
        data class OnFinish(val result: MediaChooserResultContract.MediaChooserResult) : Event()
    }

    sealed class State : CameraApp() {
        object Idling : State()
        data class WaitingForChosen(val apps: List<Components>, val path: AppFilePath) : State()
        data class Selected(val app: Components, val path: AppFilePath) : State()
        data class Finished(val app: Components, val path: AppFilePath) : State()
    }

    companion object {
        fun State.transition(event: Event): State = stateGraph.transition(this, event)

        private val stateGraph = StateGraph.create<State, Event> {
            state<State.Idling> {
                accept<Event.CandidateQueried> { State.WaitingForChosen(it.apps, it.path) }
                doNotCare<Event.OnFinish>()
            }
            state<State.WaitingForChosen> {
                accept<Event.Chosen> {
                    if (apps.contains(it.app)) State.Selected(it.app, path) else State.Idling
                }
                accept<Event.CandidateQueried> { State.WaitingForChosen(it.apps, it.path) }
                accept<Event.OnFinish> { State.Idling }
            }
            state<State.Selected> {
                accept<Event.OnFinish> { State.Finished(app, path) }
            }
            state<State.Finished> {
                accept<Event.CandidateQueried> { State.WaitingForChosen(it.apps, it.path) }
            }
        }
    }
}

data class Components(
    val packageName: String,
    val className: String
) {
    companion object
}
