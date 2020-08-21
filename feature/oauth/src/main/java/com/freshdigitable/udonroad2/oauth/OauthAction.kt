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

import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.filterByType
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import javax.inject.Inject

class OauthAction @Inject constructor(
    internal val dispatcher: EventDispatcher
) {
    val showAuth: AppAction<OauthEvent.Init> = dispatcher.toAction {
        filterByType()
    }
    internal val authApp: AppAction<OauthEvent.LoginClicked> = dispatcher.toAction {
        filterByType()
    }
    internal val inputPin: AppAction<OauthEvent.PinTextChanged> = dispatcher.toAction {
        filterByType()
    }
    internal val sendPin: AppAction<OauthEvent.SendPinClicked> = dispatcher.toAction {
        filterByType()
    }
    val authSuccess: AppAction<OauthEvent.OauthSucceeded> = dispatcher.toAction {
        filterByType()
    }
}
