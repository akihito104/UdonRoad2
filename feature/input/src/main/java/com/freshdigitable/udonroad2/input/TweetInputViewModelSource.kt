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

package com.freshdigitable.udonroad2.input

import android.text.Editable
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.input.CameraApp.Companion.transition
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toIdling
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toOpened
import com.freshdigitable.udonroad2.input.InputViewState.Companion.transTaskState
import com.freshdigitable.udonroad2.input.MediaChooserResultContract.MediaChooserResult
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.LoadingResult
import com.freshdigitable.udonroad2.model.app.ioContext
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

internal class TweetInputActions @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : TweetInputEventListener {
    override val openInput = eventDispatcher.toAction(TweetInputEvent.Open)
    override val startInput = eventDispatcher.toAction(TweetInputEvent.Opened)
    override val updateText = eventDispatcher.toAction { editable: Editable ->
        TweetInputEvent.TextUpdated(editable.toString())
    }
    override val sendTweet = eventDispatcher.toAction { tweet: InputTweet ->
        TweetInputEvent.Send(tweet)
    }
    override val cancelInput = eventDispatcher.toAction(TweetInputEvent.Cancel)

    internal val cameraApp: Flow<CameraApp.Event> = eventDispatcher.toActionFlow()

    override fun onCameraAppCandidatesQueried(candidates: List<Components>, path: AppFilePath) {
        eventDispatcher.postEvent(CameraApp.Event.CandidateQueried(candidates, path))
    }

    override val updateMedia = eventDispatcher.toAction { result: MediaChooserResult ->
        CameraApp.Event.OnFinish(result)
    }

    internal val reply: Flow<SelectedItemShortcut.Reply> = eventDispatcher.toActionFlow()
    internal val quote: Flow<SelectedItemShortcut.Quote> = eventDispatcher.toActionFlow()
}

internal class TweetInputViewModelSource @Inject constructor(
    collapsible: Boolean,
    actions: TweetInputActions,
    createReplyText: CreateReplyTextUseCase,
    postTweet: PostTweetUseCase,
    sharedState: TweetInputSharedState,
    oauthRepository: AppSettingRepository,
    userRepository: UserDataSource,
    executor: AppExecutor,
) : TweetInputEventListener by actions {
    private val idlingState = if (collapsible) InputTaskState.IDLING else InputTaskState.OPENED

    internal val state: Flow<InputViewState> = stateSourceBuilder(
        init = InputViewState(taskState = idlingState),
        actions.openInput.onEvent { state, _ -> state.toOpened() },
        actions.reply.onEvent { state, event ->
            val replyText = createReplyText(event.tweetId)
            state.toOpened(withText = replyText, withReply = event.tweetId)
        },
        actions.quote.onEvent { state, event ->
            state.toOpened(withQuote = event.tweetId)
        },
        actions.updateText.onEvent { state, event -> state.copy(text = event.text) },
        actions.updateMedia.onEvent { state, event ->
            when (val result = event.result) {
                is MediaChooserResult.Replace ->
                    state.copy(media = result.paths)
                is MediaChooserResult.Add -> {
                    state.copy(media = state.media + result.paths)
                }
                is MediaChooserResult.Canceled -> state
            }
        },
        actions.cancelInput.flatMapLatest { flowOf(InputTaskState.CANCELED, idlingState) }
            .onEvent { state, taskState -> transitTaskState(state, taskState) },
        actions.sendTweet.flatMapLatest { postTweet(it.tweet) }
            .flatMapLatest {
                when (it) {
                    is LoadingResult.Started -> flowOf(InputTaskState.SENDING)
                    is LoadingResult.Loaded -> flowOf(InputTaskState.SUCCEEDED, idlingState)
                    is LoadingResult.Failed -> flowOf(InputTaskState.FAILED)
                }
            }
            .onEvent { state, taskState -> transitTaskState(state, taskState) },
    ).onEach {
        sharedState.taskStateSource.value = it
    }

    @ExperimentalCoroutinesApi
    internal val user: Flow<UserEntity?> = oauthRepository.currentUserIdSource
        .flatMapLatest { id ->
            userRepository.getUserSource(id)
                .mapLatest { it ?: userRepository.getUser(id) }
        }
        .flowOn(executor.ioContext)

    internal val chooserForCameraApp: Flow<CameraApp.State> = stateSourceBuilder<CameraApp.State>(
        init = CameraApp.State.Idling,
        actions.cameraApp.onEvent { state, event -> state.transition(event) }
    )

    internal val isExpanded: Flow<Boolean> = sharedState.isExpanded
    internal val expandAnimationEvent = actions.startInput

    private fun transitTaskState(state: InputViewState, taskState: InputTaskState): InputViewState {
        return when (taskState) {
            idlingState -> state.toIdling(idlingState)
            else -> state.transTaskState(taskState)
        }
    }
}

enum class InputTaskState(val isExpanded: Boolean) {
    IDLING(false),

    OPENED(true),

    SENDING(false),
    FAILED(false),

    SUCCEEDED(false),
    CANCELED(false)
}

data class InputViewState(
    val taskState: InputTaskState,
    override val text: String = "",
    override val reply: TweetId? = null,
    override val quote: TweetId? = null,
    override val media: List<AppFilePath> = emptyList(),
) : InputTweet {
    val isExpanded: Boolean
        get() = taskState.isExpanded
    val hasReply: Boolean
        get() = reply != null
    val hasQuote: Boolean
        get() = quote != null
    private val hasContent: Boolean
        get() = text.isNotBlank() || media.isNotEmpty()
    val menuItem: InputMenuItem
        get() = when (taskState) {
            InputTaskState.IDLING -> InputMenuItem.WRITE_ENABLED
            InputTaskState.OPENED -> {
                if (hasContent) InputMenuItem.SEND_ENABLED else InputMenuItem.SEND_DISABLED
            }
            InputTaskState.SENDING -> InputMenuItem.SEND_DISABLED
            InputTaskState.FAILED -> InputMenuItem.RETRY_ENABLED
            InputTaskState.SUCCEEDED,
            InputTaskState.CANCELED,
            -> InputMenuItem.SEND_DISABLED
        }

    internal companion object {
        fun InputViewState.toIdling(idling: InputTaskState): InputViewState = this.copy(
            taskState = idling,
            text = "",
            reply = null,
            quote = null,
            media = emptyList()
        )

        fun InputViewState.toOpened(
            withText: String = this.text,
            withReply: TweetId? = this.reply,
            withQuote: TweetId? = this.quote,
        ): InputViewState = this.copy(
            taskState = InputTaskState.OPENED,
            text = withText,
            reply = withReply,
            quote = withQuote
        )

        fun InputViewState.transTaskState(taskState: InputTaskState): InputViewState =
            copy(taskState = taskState)
    }
}
