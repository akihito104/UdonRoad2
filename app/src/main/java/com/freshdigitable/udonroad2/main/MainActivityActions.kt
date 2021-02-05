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

package com.freshdigitable.udonroad2.main

import com.freshdigitable.udonroad2.main.MainActivityEvent.DrawerEvent
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import javax.inject.Inject

@ActivityScope
class MainActivityActions @Inject constructor(
    dispatcher: EventDispatcher,
) {
    internal val showFirstView: AppAction<TimelineEvent.Setup> = dispatcher.toAction()
    internal val showAuth: AppAction<OauthEvent.Init> = dispatcher.toAction()
    internal val showDrawerMenu: AppAction<DrawerEvent.Opened> = dispatcher.toAction()
    internal val hideDrawerMenu: AppAction<DrawerEvent.Closed> = dispatcher.toAction()
    internal val toggleAccountSwitcher: AppAction<DrawerEvent.AccountSwitchClicked> =
        dispatcher.toAction()
    internal val showCurrentUser: AppAction<MainActivityEvent.CurrentUserIconClicked> =
        dispatcher.toAction()
}

sealed class MainActivityEvent : AppEvent {
    sealed class DrawerEvent : MainActivityEvent() {
        object Opened : DrawerEvent()
        object Closed : DrawerEvent()
        object AccountSwitchClicked : DrawerEvent()
    }

    data class CurrentUserIconClicked(val user: TweetUserItem) : MainActivityEvent()
}
