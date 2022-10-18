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

import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener

internal class OauthAction(
    dispatcher: EventDispatcher,
) : ListItemLoadableEventListener, OauthEventListener {
    override val authApp = dispatcher.toAction(OauthEvent.LoginClicked)
    override val inputPin = dispatcher.toAction { pin: CharSequence ->
        OauthEvent.PinTextChanged(pin)
    }
    override val sendPin = dispatcher.toAction(OauthEvent.SendPinClicked)

    override val prependList: AppEventListener = AppEventListener.empty
    override val scrollList: AppEventListener = AppEventListener.empty
    override val stopScrollingList: AppEventListener1<Int> = AppEventListener1.empty()
    override val heading: AppEventListener = AppEventListener.empty
    override val listVisible: AppEventListener1<Boolean> = AppEventListener1.empty()
}
