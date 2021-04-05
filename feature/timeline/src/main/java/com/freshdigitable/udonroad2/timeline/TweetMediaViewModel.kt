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
    fun onMediaItemClicked(item: TweetElement, index: Int)
}

internal interface TweetMediaAction : TweetMediaEventListener, Flow<TimelineEvent.MediaItemClicked>

internal class LaunchMediaViewerAction @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : TweetMediaAction,
    Flow<TimelineEvent.MediaItemClicked> by eventDispatcher.toActionFlow() {
    override fun onMediaItemClicked(item: TweetElement, index: Int) {
        eventDispatcher.postEvent(TimelineEvent.MediaItemClicked(item.id, index))
    }
}

interface TweetMediaViewModelSource : TweetMediaEventListener, ActivityEventStream {
    val mediaState: Flow<TweetMediaItemViewModel.State>

    companion object {
        internal fun create(
            actions: TweetMediaAction,
            appSettingRepository: AppSettingRepository,
        ): TweetMediaViewModelSource = TweetMediaViewModelSourceImpl(actions, appSettingRepository)
    }
}

private class TweetMediaViewModelSourceImpl(
    launchMediaViewer: TweetMediaAction,
    appSettingRepository: AppSettingRepository,
) : TweetMediaViewModelSource,
    TweetMediaEventListener by launchMediaViewer,
    ActivityEventStream by ActivityEventStream.EmptyStream {

    override val mediaState: Flow<TweetMediaItemViewModel.State> = stateSourceBuilder(
        init = Snapshot(),
        appSettingRepository.isPossiblySensitiveHidden.onEvent { s, e ->
            s.copy(isPossiblySensitiveHidden = e)
        },
    )

    override val navigationEvent: Flow<TimelineEvent.Navigate.MediaViewer> =
        launchMediaViewer.map { TimelineEvent.Navigate.MediaViewer(it) }

    data class Snapshot(
        override val isPossiblySensitiveHidden: Boolean = false,
    ) : TweetMediaItemViewModel.State
}
