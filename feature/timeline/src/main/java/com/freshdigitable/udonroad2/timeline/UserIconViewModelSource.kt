/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UserIconClickListener {
    fun onUserIconClicked(user: TweetUserItem)
}

interface LaunchUserInfoAction : UserIconClickListener {
    val launchUserInfo: Flow<TimelineEvent.UserIconClicked>
}

@ActivityScope
class UserIconClickedAction @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : LaunchUserInfoAction {
    override val launchUserInfo: Flow<TimelineEvent.UserIconClicked> =
        eventDispatcher.toActionFlow()

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }
}

@ActivityScope
class UserIconViewModelSource @Inject constructor(
    actions: UserIconClickedAction,
) : UserIconClickListener by actions {
    val navEvent: Flow<NavigationEvent> = actions.launchUserInfo
        .map { TimelineEvent.Navigate.UserInfo(it.user) }
}
