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

import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener

internal class OauthAction(
    private val dispatcher: EventDispatcher
) : ListItemLoadableEventListener, OauthEventListener {
    override fun onLoginClicked() {
        dispatcher.postEvent(OauthEvent.LoginClicked)
    }

    override fun onAfterPinTextChanged(pin: CharSequence) {
        dispatcher.postEvent(OauthEvent.PinTextChanged(pin))
    }

    override fun onSendPinClicked() {
        dispatcher.postEvent(OauthEvent.SendPinClicked)
    }

    internal val authApp = dispatcher.toActionFlow<OauthEvent.LoginClicked>()
    internal val inputPin = dispatcher.toActionFlow<OauthEvent.PinTextChanged>()
    internal val sendPin = dispatcher.toActionFlow<OauthEvent.SendPinClicked>()

    override fun onRefresh() = Unit
    override fun onListScrollStarted() = Unit
    override fun onListScrollStopped(firstVisibleItemPosition: Int) = Unit
    override fun onHeadingClicked() = Unit
}
