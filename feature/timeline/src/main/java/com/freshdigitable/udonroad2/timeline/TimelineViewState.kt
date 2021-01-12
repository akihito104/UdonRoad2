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

import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.StateHolder
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEventDelegate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class TimelineViewState(
    owner: ListOwner<*>,
    actions: TimelineActions,
    selectedItemRepository: SelectedItemRepository,
    tweetRepository: TweetRepository,
    listOwnerGenerator: ListOwnerGenerator,
    executor: AppExecutor,
) : ShortcutViewStates by ShortcutViewStates.create(actions, tweetRepository, executor) {
    private val _selectedItemId: AppViewState<StateHolder<SelectedItemId>> = AppAction.merge(
        AppAction.just(owner).map {
            StateHolder(selectedItemRepository.find(it))
        },
        actions.selectItem
            .filter { owner == it.owner }
            .map {
                selectedItemRepository.put(it.selectedItemId)
                StateHolder(selectedItemRepository.find(it.owner))
            },
        actions.unselectItem
            .filter { owner == it.owner }
            .map {
                selectedItemRepository.remove(it.owner)
                StateHolder(null)
            },
        actions.toggleItem
            .filter { owner == it.owner }
            .map {
                val current = selectedItemRepository.find(it.item.owner)
                when (it.item) {
                    current -> selectedItemRepository.remove(it.item.owner)
                    else -> selectedItemRepository.put(it.item)
                }
                StateHolder(selectedItemRepository.find(it.owner))
            }
    ).toViewState()

    val selectedItemId: AppViewState<SelectedItemId?> = _selectedItemId.map { it.value }

    internal val updateNavHost: Flow<TimelineEvent.Navigate> = merge(
        actions.showTimeline.asFlow().map {
            listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Timeline(),
                NavigationEvent.Type.INIT
            )
        },
        actions.showTweetDetail.asFlow().map { TimelineEvent.Navigate.Detail(it.tweetId) },
        actions.showConversation.asFlow().map {
            listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Conversation(it.tweetId),
                NavigationEvent.Type.NAVIGATE
            )
        },
        actions.launchUserInfo.asFlow().map { TimelineEvent.Navigate.UserInfo(it.user) },
        actions.launchMediaViewer.asFlow().filter { it.selectedItemId?.owner == owner }
            .map { TimelineEvent.Navigate.MediaViewer(it) }
    )
}

class TimelineNavigationDelegate @Inject constructor(
    activityEventDelegate: ActivityEventDelegate,
) : ListItemFragmentEventDelegate,
    ActivityEventDelegate by activityEventDelegate
