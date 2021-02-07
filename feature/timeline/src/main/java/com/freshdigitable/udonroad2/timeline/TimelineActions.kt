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

package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.filterByType
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.timeline.TimelineEvent.Init
import com.freshdigitable.udonroad2.timeline.TimelineEvent.MediaItemClicked
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import com.freshdigitable.udonroad2.timeline.TimelineEvent.UserIconClicked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow

class TimelineActions(
    dispatcher: EventDispatcher,
) : ListItemLoadableActions by ListItemLoadableActions.create(dispatcher),
    UserIconClickedAction by UserIconClickedAction.create(dispatcher),
    LaunchMediaViewerAction by LaunchMediaViewerAction.create(dispatcher),
    ShortcutActions by ShortcutActions.create(dispatcher) {

    val showTimeline: AppAction<Init> = dispatcher.toAction()

    val selectItem: AppAction<TweetItemSelection.Selected> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TweetItemSelection.Selected>(),
            launchMediaViewer
                .filter { it.selectedItemId != null }
                .map { TweetItemSelection.Selected(requireNotNull(it.selectedItemId)) }
        )
    }
    val toggleItem: AppAction<TweetItemSelection.Toggle> = dispatcher.toAction()
    val unselectItem: AppAction<TweetItemSelection.Unselected> = AppAction.merge(
        dispatcher.toAction(),
        heading.map { TweetItemSelection.Unselected(it.owner) }
    )
}

interface UserIconClickedAction {
    val launchUserInfo: AppAction<UserIconClicked>

    companion object {
        fun create(
            eventDispatcher: EventDispatcher
        ): UserIconClickedAction = object : UserIconClickedAction {
            override val launchUserInfo: AppAction<UserIconClicked> = eventDispatcher.toAction()
        }
    }
}

interface UserIconClickedNavigation {
    val navEvent: Flow<NavigationEvent>

    companion object {
        fun create(actions: UserIconClickedAction): UserIconClickedNavigation {
            return object : UserIconClickedNavigation {
                override val navEvent: Flow<NavigationEvent> = actions.launchUserInfo.asFlow()
                    .map { TimelineEvent.Navigate.UserInfo(it.user) }
            }
        }
    }
}

interface LaunchMediaViewerAction {
    val launchMediaViewer: AppAction<MediaItemClicked>

    companion object {
        fun create(
            eventDispatcher: EventDispatcher
        ): LaunchMediaViewerAction = object : LaunchMediaViewerAction {
            override val launchMediaViewer: AppAction<MediaItemClicked> = eventDispatcher.toAction()
        }
    }
}
