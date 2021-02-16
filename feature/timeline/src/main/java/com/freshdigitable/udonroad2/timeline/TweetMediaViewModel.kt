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
import androidx.lifecycle.asLiveData
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow

interface TweetMediaItemViewModel {
    val isPossiblySensitiveHidden: LiveData<Boolean>

    fun onMediaItemClicked(originalId: TweetId, item: TweetElement, index: Int) {
        onMediaItemClicked(originalId, null, item, index)
    }

    fun onMediaItemClicked(originalId: TweetId, quotedId: TweetId?, item: TweetElement, index: Int)

    companion object {
        fun create(
            eventDispatcher: EventDispatcher,
            viewStates: TweetMediaViewStates
        ): TweetMediaItemViewModel = TweetMediaItemViewModelImpl(eventDispatcher, viewStates)
    }
}

interface LaunchMediaViewerAction {
    val launchMediaViewer: AppAction<TimelineEvent.MediaItemClicked>

    companion object {
        fun create(
            eventDispatcher: EventDispatcher
        ): LaunchMediaViewerAction = object : LaunchMediaViewerAction {
            override val launchMediaViewer: AppAction<TimelineEvent.MediaItemClicked> =
                eventDispatcher.toAction()
        }
    }
}

interface TweetMediaViewStates : ActivityEventStream {
    val isPossiblySensitiveHidden: LiveData<Boolean>

    companion object {
        fun create(
            actions: LaunchMediaViewerAction,
            appSettingRepository: AppSettingRepository,
            coroutineScope: CoroutineScope
        ): TweetMediaViewStates =
            TweetMediaViewStatesImpl(actions, appSettingRepository, coroutineScope)
    }
}

private class TweetMediaItemViewModelImpl(
    private val eventDispatcher: EventDispatcher,
    viewStates: TweetMediaViewStates,
) : TweetMediaItemViewModel {
    override val isPossiblySensitiveHidden: LiveData<Boolean> = viewStates.isPossiblySensitiveHidden

    override fun onMediaItemClicked(
        originalId: TweetId,
        quotedId: TweetId?,
        item: TweetElement,
        index: Int
    ) {
        eventDispatcher.postEvent(TimelineEvent.MediaItemClicked(item.id, index))
    }
}

private class TweetMediaViewStatesImpl(
    actions: LaunchMediaViewerAction,
    appSettingRepository: AppSettingRepository,
    coroutineScope: CoroutineScope,
) : TweetMediaViewStates, ActivityEventStream by ActivityEventStream.EmptyStream {

    override val isPossiblySensitiveHidden: LiveData<Boolean> =
        appSettingRepository.isPossiblySensitiveHidden.asLiveData(coroutineScope.coroutineContext)

    override val navigationEvent: Flow<TimelineEvent.Navigate.MediaViewer> =
        actions.launchMediaViewer.asFlow().map { TimelineEvent.Navigate.MediaViewer(it) }
}
