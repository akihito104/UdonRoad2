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
import com.freshdigitable.udonroad2.model.app.navigation.AppAction1
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface UserIconClickListener {
    val launchUserInfo: AppEventListener1<TweetUserItem>
}

interface LaunchUserInfoAction : UserIconClickListener {
    override val launchUserInfo: AppAction1<TweetUserItem, TimelineEvent.UserIconClicked>
}

@ActivityScope
class UserIconClickedAction @Inject constructor(
    eventDispatcher: EventDispatcher,
) : LaunchUserInfoAction {
    override val launchUserInfo = eventDispatcher.toAction { item: TweetUserItem ->
        TimelineEvent.UserIconClicked(item)
    }
}

@ActivityScope
class UserIconViewModelSource @Inject constructor(
    actions: UserIconClickedAction,
) : UserIconClickListener by actions {
    val navEvent: Flow<AppEffect.Navigation> = actions.launchUserInfo
        .map { TimelineEffect.Navigate.UserInfo(it.user) }
}
