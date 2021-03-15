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
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.MediaRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.ext.combineLatest
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.navigation.SnackbarFeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.shortcut.ShortcutEventListener
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.shortcut.ShortcutViewStates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject
import kotlin.math.min

class MediaViewModel @Inject constructor(
    val tweetId: TweetId,
    private val eventDispatcher: EventDispatcher,
    viewStates: MediaViewModelViewStates,
) : ShortcutViewModel,
    ShortcutEventListener by ShortcutEventListener.create(eventDispatcher),
    ViewModel() {
    val mediaItems: LiveData<List<MediaEntity>> = viewStates.mediaItems
    override val isFabVisible: LiveData<Boolean> = viewStates.isFabVisible
    internal val systemUiVisibility: LiveData<SystemUiVisibility> = viewStates.systemUiVisibility
    internal val messageEvent: Flow<FeedbackMessage> = viewStates.updateTweet

    internal fun onSystemUiVisibilityChange(visibility: Int) {
        eventDispatcher.postEvent(
            MediaViewerEvent.SystemUiVisibilityChanged(SystemUiVisibility.get(visibility))
        )
    }

    fun toggleUiVisibility() {
        eventDispatcher.postEvent(
            MediaViewerEvent.SystemUiVisibilityToggled(checkNotNull(systemUiVisibility.value))
        )
    }

    val currentPosition: LiveData<Int?> = viewStates.currentPosition

    fun setCurrentPosition(pos: Int) {
        eventDispatcher.postEvent(MediaViewerEvent.CurrentPositionChanged(pos))
    }
}

internal sealed class MediaViewerEvent : AppEvent {
    data class CurrentPositionChanged(val index: Int) : MediaViewerEvent()

    data class SystemUiVisibilityChanged(val visibility: SystemUiVisibility) : MediaViewerEvent()
    data class SystemUiVisibilityToggled(val currentVisibility: SystemUiVisibility) :
        MediaViewerEvent()
}

class MediaViewModelActions @Inject constructor(
    eventDispatcher: EventDispatcher
) : ShortcutActions by ShortcutActions.create(eventDispatcher) {
    internal val changeCurrentPosition: AppAction<MediaViewerEvent.CurrentPositionChanged> =
        eventDispatcher.toAction()
    internal val changeSystemUiVisibility: AppAction<MediaViewerEvent.SystemUiVisibilityChanged> =
        eventDispatcher.toAction()
    internal val toggleSystemUiVisibility: AppAction<MediaViewerEvent.SystemUiVisibilityToggled> =
        eventDispatcher.toAction()
}

class MediaViewModelViewStates @Inject constructor(
    tweetId: TweetId,
    firstPosition: Int,
    actions: MediaViewModelActions,
    repository: MediaRepository,
    tweetRepository: TweetRepository,
    executor: AppExecutor,
) : ShortcutViewStates by ShortcutViewStates.create(actions, tweetRepository) {
    internal val mediaItems: AppViewState<List<MediaEntity>> =
        repository.getMediaItemSource(tweetId)
            .transformLatest {
                if (it.isNotEmpty()) {
                    emit(it)
                } else {
                    tweetRepository.findDetailTweetItem(tweetId)
                }
            }
            .asLiveData(executor.dispatcher.mainContext)

    internal val systemUiVisibility: AppViewState<SystemUiVisibility> = AppAction.merge(
        AppAction.just(SystemUiVisibility.SHOW),
        actions.changeSystemUiVisibility.map { it.visibility },
        actions.toggleSystemUiVisibility.map { it.currentVisibility.toggle() }
    )
        .distinctUntilChanged()
        .toViewState()
    internal val isFabVisible: LiveData<Boolean> = systemUiVisibility.map {
        it == SystemUiVisibility.SHOW
    }

    internal val currentPosition: AppViewState<Int?> = combineLatest(
        AppAction.merge(
            AppAction.just(firstPosition),
            actions.changeCurrentPosition.map { it.index }
        ).distinctUntilChanged().toViewState<Int, Int>(),
        mediaItems
    ) { pos, items ->
        if (pos != null && pos < 0) {
            0
        } else if (pos != null && items?.isNotEmpty() == true) {
            min(pos, items.size - 1)
        } else {
            value
        }
    }
}

internal class MediaActivityEventDelegate @Inject constructor(
    activity: MediaActivity
) : ActivityEventDelegate,
    FeedbackMessageDelegate by SnackbarFeedbackMessageDelegate(
        weakRef(activity) { it.findViewById(R.id.media_container) }
    )

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
