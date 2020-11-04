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
}

sealed class TweetInputEvent : AppEvent {
    object Open : TweetInputEvent()
    object Opened : TweetInputEvent()
    data class Send(val text: String) : TweetInputEvent()
    object Cancel : TweetInputEvent()

    data class TextUpdated(val text: String) : TweetInputEvent()
}
