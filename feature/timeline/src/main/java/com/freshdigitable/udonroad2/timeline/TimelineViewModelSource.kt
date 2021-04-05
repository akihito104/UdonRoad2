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

import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEventDelegate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

internal class TimelineViewModelSource(
    owner: ListOwner<QueryType.TweetQueryType>,
    actions: TimelineActions,
    selectedItemRepository: SelectedItemRepository,
    tweetRepository: TweetRepository,
    listOwnerGenerator: ListOwnerGenerator,
    private val baseViewModelSource: ListItemLoadableViewModelSource,
    mediaViewModelSource: TweetMediaViewModelSource,
) : ListItemLoadableViewModelSource by baseViewModelSource,
    TweetMediaViewModelSource by mediaViewModelSource,
    ActivityEventStream,
    ShortcutViewStates by ShortcutViewStates.create(actions, tweetRepository),
    TweetListItemEventListener by actions {

    override val state: Flow<TimelineState> = stateSourceBuilder(
        init = TimelineState(
            selectedItemId = selectedItemRepository.find(owner)
        ),
        baseViewModelSource.state.onEvent { s, base -> s.copy(baseState = base) },
        actions.selectItem.onEvent { s, e -> s.copy(selectedItemId = e.selectedItemId) },
        actions.unselectItem.onEvent { s, _ -> s.copy(selectedItemId = null) },
        actions.toggleItem.onEvent { s, e ->
            when (s.selectedItemId) {
                e.item -> s.copy(selectedItemId = null)
                else -> s.copy(selectedItemId = e.item)
            }
        },
        actions.heading.onEvent { s, _ -> s.copy(selectedItemId = null) },
    ).onEach {
        if (it.selectedItemId != null) {
            selectedItemRepository.put(it.selectedItemId)
        } else {
            selectedItemRepository.remove(owner)
        }
    }

    internal val selectedItemId: Flow<SelectedItemId?> = selectedItemRepository.getSource(owner)

    override val navigationEvent: Flow<NavigationEvent> = merge(
        actions.showTimeline.map {
            listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Timeline(),
                NavigationEvent.Type.INIT
            )
        },
        actions.showTweetDetail.map { TimelineEvent.Navigate.Detail(it.tweetId) },
        actions.showConversation.map {
            listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Conversation(it.tweetId),
                NavigationEvent.Type.NAVIGATE
            )
        },
        mediaViewModelSource.navigationEvent,
        baseViewModelSource.navigationEvent,
    )
    override val feedbackMessage: Flow<FeedbackMessage> = updateTweet

    override suspend fun clear() {
        super.clear()
        baseViewModelSource.clear()
    }
}

data class TimelineState(
    val baseState: ListItemLoadableViewModel.State? = null,
    val selectedItemId: SelectedItemId? = null,
) : ListItemLoadableViewModel.State {
    override val isHeadingEnabled: Boolean
        get() = baseState?.isHeadingEnabled == true || selectedItemId != null
}

class TimelineNavigationDelegate @Inject constructor(
    activityEventDelegate: ActivityEventDelegate,
) : ListItemFragmentEventDelegate,
    ActivityEventDelegate by activityEventDelegate
