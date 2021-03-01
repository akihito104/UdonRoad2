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

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.timeline.TimelineEvent.Init
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import com.freshdigitable.udonroad2.timeline.TimelineEvent.UserIconClicked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

internal class TimelineActions(
    private val owner: ListOwner<*>,
    private val dispatcher: EventDispatcher,
    listItemLoadableActions: ListItemLoadableAction,
) : ListItemLoadableAction by listItemLoadableActions,
    UserIconClickedAction by UserIconClickedAction.create(dispatcher),
    ShortcutActions by ShortcutActions.create(dispatcher),
    TweetListItemEventListener {

    val showTimeline: AppAction<Init> = dispatcher.toAction()
    val selectItem = dispatcher.toActionFlow<TweetItemSelection.Selected>()
        .filter { it.owner == owner }
    val toggleItem = dispatcher.toActionFlow<TweetItemSelection.Toggle>()
        .filter { it.owner == owner }
    val unselectItem = dispatcher.toActionFlow<TweetItemSelection.Unselected>()
        .filter { it.owner == owner }

    override fun onBodyItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.body.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId))
    }

    override fun onQuoteItemClicked(item: TweetListItem) {
        Timber.tag("TimelineViewModel").d("onQuoteItemClicked: ${item.quoted?.id}")
        updateSelectedItem(SelectedItemId(owner, item.originalId, item.quoted?.id))
    }

    private fun updateSelectedItem(selected: SelectedItemId) {
        dispatcher.postEvent(TweetItemSelection.Toggle(selected))
    }

    override fun onUserIconClicked(user: TweetUserItem) {
        dispatcher.postEvent(UserIconClicked(user))
    }
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
