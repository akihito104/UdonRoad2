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

import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.fabshortcut.FlingFAB
import com.freshdigitable.udonroad2.data.impl.MediaRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.media.MediaViewModelViewStates.Snapshot.Companion.updateScale
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.shortcut.MenuItemState
import com.freshdigitable.udonroad2.shortcut.ShortcutEventListener
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModelSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.math.min

internal class MediaViewModel @Inject constructor(
    viewStates: MediaViewModelViewStates,
    shortcutViewModelSource: ShortcutViewModelSource,
) : MediaEventListener by viewStates,
    ShortcutViewModel,
    ShortcutEventListener by shortcutViewModelSource,
    ActivityEffectStream by shortcutViewModelSource,
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
        override val mode: FlingFAB.Mode
            get() = when (systemUiVisibility) {
                SystemUiVisibility.SHOW -> FlingFAB.Mode.FAB
                else -> FlingFAB.Mode.HIDDEN
            }
        val isUserInputEnabled: Boolean
    }
}

interface MediaEventListener : ScalableImageView.ScaleEventListener {
    val changeSystemUiVisibility: AppEventListener1<Int>
    val toggleSystemUiVisibility: AppEventListener
    val changeCurrentPosition: AppEventListener1<Int>
    val changePictureScale: AppEventListener1<Scale>
    val changePictureArea: AppEventListener1<Scroll>

    override fun onScale(scale: Float, xFocus: Float, yFocus: Float) {
        changePictureScale.dispatch(Scale(scale, xFocus, yFocus))
    }

    override fun onScroll(xScroll: Float, yScroll: Float) {
        changePictureArea.dispatch(Scroll(xScroll, yScroll))
    }

    data class Scale(val scale: Float, val xFocus: Float, val yFocus: Float)
    data class Scroll(val xScroll: Float, val yScroll: Float)
}

internal class MediaViewModelActions @Inject constructor(
    eventDispatcher: EventDispatcher,
) : MediaEventListener {
    internal sealed class Event : AppEvent {
        data class CurrentPositionChanged(val index: Int) : Event()
        data class SystemUiVisibilityChanged(val visibility: SystemUiVisibility) : Event()
        object SystemUiVisibilityToggled : Event()
        data class PictureScaleChanged(val scale: MediaEventListener.Scale) : Event()
        data class PictureAreaChanged(val scroll: MediaEventListener.Scroll) : Event()
    }

    override val changeSystemUiVisibility = eventDispatcher.toAction { visibility: Int ->
        Event.SystemUiVisibilityChanged(SystemUiVisibility.get(visibility))
    }
    override val toggleSystemUiVisibility =
        eventDispatcher.toAction(Event.SystemUiVisibilityToggled)
    override val changeCurrentPosition = eventDispatcher.toAction { pos: Int ->
        Event.CurrentPositionChanged(pos)
    }
    override val changePictureScale = eventDispatcher.toAction { scale: MediaEventListener.Scale ->
        Event.PictureScaleChanged(scale)
    }
    override val changePictureArea = eventDispatcher.toAction { scroll: MediaEventListener.Scroll ->
        Event.PictureAreaChanged(scroll)
    }
}

internal class MediaViewModelViewStates @Inject constructor(
    tweetId: TweetId,
    firstPosition: Int,
    actions: MediaViewModelActions,
    repository: MediaRepository,
    tweetRepository: TweetRepository,
) : MediaEventListener by actions {
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
        actions.changeCurrentPosition.onEvent { s, e -> s.copy(position = e.index) },
        actions.changePictureScale.onEvent { s, e -> s.updateScale(e.scale) }
    )

    private data class Snapshot(
        override val tweetId: TweetId,
        override val mediaItems: List<MediaEntity> = emptyList(),
        override val systemUiVisibility: SystemUiVisibility = SystemUiVisibility.SHOW,
        private val position: Int,
        val scale: List<MediaEventListener.Scale?> = emptyList(),
    ) : MediaViewModel.State {
        override val currentPosition: Int?
            get() = when {
                mediaItems.isNotEmpty() -> min(position.coerceAtLeast(0), mediaItems.lastIndex)
                else -> null
            }
        override val menuItemState: MenuItemState = MenuItemState()
        override val isUserInputEnabled: Boolean
            get() {
                if (mediaItems.size <= 1) {
                    return false
                }
                val p = currentPosition ?: return false
                val s = scale.getOrNull(p) ?: return true
                return s.scale < 1.05f
            }

        companion object {
            fun Snapshot.updateScale(scale: MediaEventListener.Scale): Snapshot {
                val p = currentPosition ?: return this
                return this.copy(
                    scale = List(mediaItems.size) {
                        if (it == p) scale else this.scale.getOrNull(it)
                    }
                )
            }
        }
    }
}

enum class SystemUiVisibility {
    SHOW,
    HIDE,
    ;

    val visibility: Int get() = windowInsetsType
    fun toggle(): SystemUiVisibility = when (this) {
        SHOW -> HIDE
        else -> SHOW
    }

    companion object {
        private val windowInsetsType: Int
            get() =
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars()
        private val FULLSCREEN = windowInsetsType

        fun get(visibility: Int): SystemUiVisibility = when {
            visibility and FULLSCREEN == 0 -> SHOW
            else -> HIDE
        }
    }
}
