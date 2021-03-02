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
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.input.CameraApp.Companion.transition
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toCanceled
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toFailed
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toIdling
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toOpened
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toSending
import com.freshdigitable.udonroad2.input.InputViewState.Companion.toSucceeded
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.ioContext
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.scanSource
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import java.io.IOException
import javax.inject.Inject

internal class TweetInputActions @Inject constructor(
    private val eventDispatcher: EventDispatcher
) : TweetInputEventListener {
    override fun onWriteClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Open)
    }

    override fun onTweetTextChanged(text: Editable) {
        eventDispatcher.postEvent(TweetInputEvent.TextUpdated(text.toString()))
    }

    override fun onSendClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Send)
    }

    override fun onCancelClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Cancel)
    }

    override fun onExpandAnimationEnd() {
        eventDispatcher.postEvent(TweetInputEvent.Opened)
    }

    override fun onCameraAppCandidatesQueried(candidates: List<Components>, path: AppFilePath) {
        eventDispatcher.postEvent(CameraApp.Event.CandidateQueried(candidates, path))
    }

    override fun onMediaChooserFinished(result: MediaChooserResultContract.MediaChooserResult) {
        eventDispatcher.postEvent(CameraApp.Event.OnFinish(result))
    }

    internal val openInput: Flow<TweetInputEvent.Open> = eventDispatcher.toActionFlow()
    internal val reply: Flow<SelectedItemShortcut.Reply> = eventDispatcher.toActionFlow()
    internal val quote: Flow<SelectedItemShortcut.Quote> = eventDispatcher.toActionFlow()
    internal val sendTweet: Flow<TweetInputEvent.Send> = eventDispatcher.toActionFlow()
    internal val cancelInput: Flow<TweetInputEvent.Cancel> = eventDispatcher.toActionFlow()
    internal val updateText: Flow<TweetInputEvent.TextUpdated> = eventDispatcher.toActionFlow()
    internal val cameraApp: Flow<CameraApp.Event> = eventDispatcher.toActionFlow()
    internal val updateMedia: Flow<CameraApp.Event.OnFinish> = eventDispatcher.toActionFlow()
}

internal class TweetInputViewModelSource @Inject constructor(
    collapsible: Boolean,
    actions: TweetInputActions,
    createReplyText: CreateReplyTextUseCase,
    createQuoteText: CreateQuoteTextUseCase,
    sharedState: TweetInputSharedState,
    repository: TweetInputRepository,
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
                is MediaChooserResultContract.MediaChooserResult.Replace ->
                    state.copy(media = result.paths)
                is MediaChooserResultContract.MediaChooserResult.Add -> {
                    state.copy(media = state.media + result.paths)
                }
                is MediaChooserResultContract.MediaChooserResult.Canceled -> state
            }
        },
        actions.cancelInput.onEvent(
            atFirst = scanSource { state, _ -> state.toCanceled() }
        ) {
            onNext { state, _ -> state.toIdling(idlingState) }
        },
        actions.sendTweet.onEvent(
            atFirst = scanSource { state, _ -> state.toSending() }
        ) {
            onNext(
                withResult = { state, _ ->
                    val mediaIds = state.media.map { repository.uploadMedia(it) }
                    val quoteText = state.quote?.let { createQuoteText(it) }
                    val text = if (quoteText == null) state.text else "${state.text} $quoteText"
                    kotlin.runCatching {
                        repository.post(text, mediaIds, state.reply)
                    }
                },
                onSuccess = listOf(
                    { state, _ -> state.toSucceeded() },
                    { state, _ -> state.toIdling(idlingState) }
                ),
                onError = listOf { state, exception ->
                    if (exception is AppTwitterException || exception is IOException) {
                        state.toFailed()
                    } else {
                        throw exception
                    }
                }
            )
        }
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
    val text: String = "",
    val reply: TweetId? = null,
    val quote: TweetId? = null,
    val media: List<AppFilePath> = emptyList(),
) {
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
            InputTaskState.CANCELED -> InputMenuItem.SEND_DISABLED
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

        fun InputViewState.toCanceled(): InputViewState = copy(taskState = InputTaskState.CANCELED)
        fun InputViewState.toSending(): InputViewState = copy(taskState = InputTaskState.SENDING)
        fun InputViewState.toSucceeded(): InputViewState =
            copy(taskState = InputTaskState.SUCCEEDED)

        fun InputViewState.toFailed(): InputViewState = copy(taskState = InputTaskState.FAILED)
    }
}
