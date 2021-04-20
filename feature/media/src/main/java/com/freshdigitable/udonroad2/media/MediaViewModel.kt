/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.media

import android.os.Build
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.impl.MediaRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.shortcut.ShortcutEventListener
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.math.min

internal class MediaViewModel @Inject constructor(
    eventDispatcher: EventDispatcher,
    viewStates: MediaViewModelViewStates,
) : MediaEventListener by viewStates,
    ShortcutViewModel,
    ShortcutEventListener by ShortcutEventListener.create(eventDispatcher),
    ActivityEffectStream by viewStates,
    ViewModel() {
    val state = viewStates.state.asLiveData(viewModelScope.coroutineContext)
    internal val mediaItems: LiveData<List<MediaEntity>> = state.map { it.mediaItems }
        .distinctUntilChanged()

    @Suppress("UNCHECKED_CAST")
    override val shortcutState: LiveData<ShortcutViewModel.State> =
        state as LiveData<ShortcutViewModel.State>
    internal val systemUiVisibility: LiveData<SystemUiVisibility> =
        state.map { it.systemUiVisibility }.distinctUntilChanged()

    interface State : ShortcutViewModel.State {
        val tweetId: TweetId
        val mediaItems: List<MediaEntity>
        val systemUiVisibility: SystemUiVisibility
        val currentPosition: Int?
        override val isVisible: Boolean
            get() = systemUiVisibility == SystemUiVisibility.SHOW
    }
}

interface MediaEventListener {
    val changeSystemUiVisibility: AppEventListener1<Int>
    val toggleSystemUiVisibility: AppEventListener
    val changeCurrentPosition: AppEventListener1<Int>
}

internal class MediaViewModelActions @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : MediaEventListener,
    ShortcutActions by ShortcutActions.create(eventDispatcher) {
    internal sealed class Event : AppEvent {
        data class CurrentPositionChanged(val index: Int) : Event()
        data class SystemUiVisibilityChanged(val visibility: SystemUiVisibility) : Event()
        object SystemUiVisibilityToggled : Event()
    }

    override val changeSystemUiVisibility = eventDispatcher.toAction { visibility: Int ->
        Event.SystemUiVisibilityChanged(SystemUiVisibility.get(visibility))
    }
    override val toggleSystemUiVisibility =
        eventDispatcher.toAction(Event.SystemUiVisibilityToggled)
    override val changeCurrentPosition = eventDispatcher.toAction { pos: Int ->
        Event.CurrentPositionChanged(pos)
    }
}

internal class MediaViewModelViewStates @Inject constructor(
    tweetId: TweetId,
    firstPosition: Int,
    actions: MediaViewModelActions,
    repository: MediaRepository,
    tweetRepository: TweetRepository,
) : MediaEventListener by actions,
    ShortcutViewStates by ShortcutViewStates.create(actions, tweetRepository),
    ActivityEffectStream {
    internal val state: Flow<MediaViewModel.State> = stateSourceBuilder(
        init = Snapshot(tweetId = tweetId, position = firstPosition),
        repository.getMediaItemSource(tweetId).onEvent { s, items ->
            if (items.isNotEmpty()) {
                s.copy(mediaItems = items)
            } else {
                tweetRepository.findDetailTweetItem(tweetId)
                s
            }
        },
        actions.changeSystemUiVisibility.onEvent { s, e ->
            s.copy(systemUiVisibility = e.visibility)
        },
        actions.toggleSystemUiVisibility.onEvent { s, _ ->
            s.copy(systemUiVisibility = s.systemUiVisibility.toggle())
        },
        actions.changeCurrentPosition.onEvent { s, e -> s.copy(position = e.index) }
    )
    override val effect: Flow<AppEffect> = updateTweet

    private data class Snapshot(
        override val tweetId: TweetId,
        override val mediaItems: List<MediaEntity> = emptyList(),
        override val systemUiVisibility: SystemUiVisibility = SystemUiVisibility.SHOW,
        private val position: Int,
    ) : MediaViewModel.State {
        override val currentPosition: Int?
            get() = when {
                mediaItems.isNotEmpty() -> min(position.coerceAtLeast(0), mediaItems.lastIndex)
                else -> null
            }
    }
}

enum class SystemUiVisibility(val visibility: Int) {
    SHOW(
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            else -> -1
        }
    ),
    HIDE(
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or // hide nav bar
                    View.SYSTEM_UI_FLAG_FULLSCREEN or // hide status bar
                    View.SYSTEM_UI_FLAG_IMMERSIVE
            }
            else -> View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
        }
    ),
    ;

    fun toggle(): SystemUiVisibility = when (this) {
        SHOW -> HIDE
        else -> SHOW
    }

    companion object {
        private val FULLSCREEN = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ->
                View.SYSTEM_UI_FLAG_FULLSCREEN
            else -> View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        fun get(visibility: Int): SystemUiVisibility = when {
            visibility and FULLSCREEN == 0 -> SHOW
            else -> HIDE
        }
    }
}
