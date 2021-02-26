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

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEventDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class TimelineViewState(
    owner: ListOwner<QueryType.TweetQueryType>,
    actions: TimelineActions,
    selectedItemRepository: SelectedItemRepository,
    tweetRepository: TweetRepository,
    appSettingRepository: AppSettingRepository,
    listOwnerGenerator: ListOwnerGenerator,
    executor: AppExecutor,
    baseViewState: ListItemLoadableViewState,
) : ListItemLoadableViewState by baseViewState,
    TweetMediaViewStates,
    ActivityEventStream,
    ShortcutViewStates by ShortcutViewStates.create(actions, tweetRepository, executor) {
    private val coroutineScope = CoroutineScope(executor.mainContext + SupervisorJob())
    private val mediaViewStates =
        TweetMediaViewStates.create(actions, appSettingRepository, coroutineScope)

    override val state: Flow<TimelineState> = stateSourceBuilder(
        init = TimelineState(
            selectedItemId = selectedItemRepository.find(owner)
        ),
        baseViewState.state.onEvent { s, e -> s.copy(baseState = e) },
        actions.selectItem.filter { owner == it.owner }.asFlow().onEvent { s, e ->
            s.copy(selectedItemId = e.selectedItemId)
        },
        actions.unselectItem.filter { owner == it.owner }.asFlow().onEvent { s, _ ->
            s.copy(selectedItemId = null)
        },
        actions.toggleItem.filter { owner == it.owner }.asFlow().onEvent { s, e ->
            if (s.selectedItemId == e.item) {
                s.copy(selectedItemId = null)
            } else {
                s.copy(selectedItemId = e.item)
            }
        },
    ).onEach {
        if (it.selectedItemId != null) {
            selectedItemRepository.put(it.selectedItemId)
        } else {
            selectedItemRepository.remove(owner)
        }
    }

    val selectedItemId: AppViewState<SelectedItemId?> =
        selectedItemRepository.getSource(owner).asLiveData(executor.mainContext)

    private val userInfoNavigation = UserIconClickedNavigation.create(actions)
    override val navigationEvent: Flow<NavigationEvent> = merge(
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
        userInfoNavigation.navEvent,
        mediaViewStates.navigationEvent
            .filterIsInstance<TimelineEvent.Navigate.MediaViewer>()
            .filter { it.selectedItemId?.owner == owner },
        baseViewState.navigationEvent,
    )
    override val feedbackMessage: Flow<FeedbackMessage> = updateTweet.asFlow()

    override val isPossiblySensitiveHidden: LiveData<Boolean> =
        mediaViewStates.isPossiblySensitiveHidden

    override suspend fun clear() {
        coroutineScope.cancel()
    }
}

data class TimelineState(
    val baseState: ListItemLoadableViewState.State? = null,
    val selectedItemId: SelectedItemId? = null,
) : ListItemLoadableViewState.State {
    override val isHeadingEnabled: Boolean
        get() = baseState?.isHeadingEnabled == true || selectedItemId != null
}

class TimelineNavigationDelegate @Inject constructor(
    activityEventDelegate: ActivityEventDelegate,
) : ListItemFragmentEventDelegate,
    ActivityEventDelegate by activityEventDelegate
