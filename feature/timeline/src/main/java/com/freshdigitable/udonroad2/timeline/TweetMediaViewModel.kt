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

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface TweetMediaItemViewModel : TweetMediaEventListener {
    val mediaState: LiveData<State>

    interface State {
        val isPossiblySensitiveHidden: Boolean
    }
}

interface TweetMediaEventListener {
    fun onMediaItemClicked(originalId: TweetId, item: TweetElement, index: Int) {
        onMediaItemClicked(originalId, null, item, index)
    }

    fun onMediaItemClicked(originalId: TweetId, quotedId: TweetId?, item: TweetElement, index: Int)
}

internal class LaunchMediaViewerAction @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : TweetMediaEventListener {
    override fun onMediaItemClicked(
        originalId: TweetId,
        quotedId: TweetId?,
        item: TweetElement,
        index: Int
    ) {
        eventDispatcher.postEvent(TimelineEvent.MediaItemClicked(item.id, index))
    }

    internal val launchMediaViewer: Flow<TimelineEvent.MediaItemClicked> =
        eventDispatcher.toActionFlow()
}

interface TweetMediaViewModelSource : TweetMediaEventListener, ActivityEventStream {
    val state: Flow<TweetMediaItemViewModel.State>

    companion object {
        internal fun create(
            actions: LaunchMediaViewerAction,
            appSettingRepository: AppSettingRepository,
            selectedItemRepository: SelectedItemRepository,
        ): TweetMediaViewModelSource =
            TweetMediaViewModelSourceImpl(actions, appSettingRepository, selectedItemRepository)
    }
}

private class TweetMediaViewModelSourceImpl(
    actions: LaunchMediaViewerAction,
    appSettingRepository: AppSettingRepository,
    selectedItemRepository: SelectedItemRepository,
) : TweetMediaViewModelSource,
    TweetMediaEventListener by actions,
    ActivityEventStream by ActivityEventStream.EmptyStream {

    override val state: Flow<TweetMediaItemViewModel.State> = stateSourceBuilder(
        init = Snapshot(),
        appSettingRepository.isPossiblySensitiveHidden.onEvent { s, e ->
            s.copy(isPossiblySensitiveHidden = e)
        },
        actions.launchMediaViewer.onEvent { s, e ->
            if (e.selectedItemId != null) {
                selectedItemRepository.put(e.selectedItemId)
            }
            s
        }
    )

    override val navigationEvent: Flow<TimelineEvent.Navigate.MediaViewer> =
        actions.launchMediaViewer.map { TimelineEvent.Navigate.MediaViewer(it) }

    data class Snapshot(
        override val isPossiblySensitiveHidden: Boolean = false
    ) : TweetMediaItemViewModel.State
}
