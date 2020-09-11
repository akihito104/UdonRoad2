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

package com.freshdigitable.udonroad2.user

import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import javax.inject.Inject

class UserActivityActions @Inject constructor(eventDispatcher: EventDispatcher) {
    val changeFollowingStatus: AppAction<UserActivityEvent.Following> = eventDispatcher.toAction()
    val changeBlockingStatus: AppAction<UserActivityEvent.Blocking> = eventDispatcher.toAction()
    val changeRetweetBlockingStatus: AppAction<UserActivityEvent.WantsRetweet> =
        eventDispatcher.toAction()
    val changeMutingStatus: AppAction<UserActivityEvent.Muting> = eventDispatcher.toAction()
    val reportSpam: AppAction<UserActivityEvent.ReportSpam> = eventDispatcher.toAction()
}
