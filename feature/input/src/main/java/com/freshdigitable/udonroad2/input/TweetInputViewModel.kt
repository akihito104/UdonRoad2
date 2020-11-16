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

package com.freshdigitable.udonroad2.input

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.input.CameraApp.Companion.transition
import com.freshdigitable.udonroad2.input.MediaChooserResultContract.MediaChooserResult
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.ioContext
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.subscribeToUpdate
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.User
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class TweetInputViewModel @Inject constructor(
    private val eventDispatcher: EventDispatcher,
    private val viewState: TweetInputViewState,
) : ViewModel() {

    val isExpanded: LiveData<Boolean> = viewState.isExpanded
    val text: LiveData<String> = viewState.text
    val media: LiveData<List<AppFilePath>> = viewState.media
    val menuItem: LiveData<InputMenuItem> = viewState.menuItem
    val inputTask: LiveData<InputTaskState> = viewState.taskState
    val expandAnimationEvent: Flow<TweetInputEvent.Opened> =
        eventDispatcher.toAction<TweetInputEvent.Opened>().asFlow()
    internal val chooserForCameraApp = viewState.chooserForCameraApp.asFlow()

    val user: LiveData<User?> = viewState.user

    fun onWriteClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Open)
    }

    fun onTweetTextChanged(text: Editable) {
        eventDispatcher.postEvent(TweetInputEvent.TextUpdated(text.toString()))
    }

    fun onSendClicked() {
        eventDispatcher.postEvent(
            TweetInputEvent.Send(checkNotNull(text.value), checkNotNull(media.value))
        )
    }

    fun onCancelClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Cancel)
    }

    fun onExpandAnimationEnd() {
        eventDispatcher.postEvent(TweetInputEvent.Opened)
    }

    fun onCameraAppCandidatesQueried(candidates: List<Components>, path: AppFilePath) {
        eventDispatcher.postEvent(CameraApp.Event.CandidateQueried(candidates, path))
    }

    internal fun onMediaChooserFinished(result: MediaChooserResult) {
        eventDispatcher.postEvent(CameraApp.Event.OnFinish(result))
    }

    override fun onCleared() {
        super.onCleared()
        viewState.clear()
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

@ActivityScope
@ExperimentalCoroutinesApi
class TweetInputSharedState @Inject constructor(executor: AppExecutor) {
    internal val taskStateSource = MutableStateFlow<InputTaskState?>(null)
    val isExpanded: LiveData<Boolean> = taskStateSource.map { it?.isExpanded == true }
        .distinctUntilChanged()
        .asLiveDataWithMain(executor)
    internal val textSource = MutableStateFlow("")
    internal val mediaSource = MutableStateFlow<List<AppFilePath>>(emptyList())
}

@ExperimentalCoroutinesApi
class TweetInputViewState @Inject constructor(
    collapsible: Boolean,
    actions: TweetInputActions,
    sharedState: TweetInputSharedState,
    repository: TweetInputRepository,
    oauthRepository: OAuthTokenRepository,
    private val tweetRepository: TweetRepository,
    private val replyRepository: ReplyRepository,
    userRepository: UserRepository,
    private val executor: AppExecutor,
) {
    private val idlingState = if (collapsible) InputTaskState.IDLING else InputTaskState.OPENED

    init {
        sharedState.taskStateSource.value = idlingState
    }

    private val hasContent: Flow<Boolean> = sharedState.textSource.combineTransform(
        sharedState.mediaSource
    ) { text, media ->
        emit(text.isNotBlank() || media.isNotEmpty())
    }.distinctUntilChanged()

    internal val menuItem: AppViewState<InputMenuItem> = sharedState.taskStateSource.filterNotNull()
        .combineTransform(hasContent) { state, hasContent ->
            val menu = when (state) {
                InputTaskState.IDLING -> InputMenuItem.WRITE_ENABLED
                InputTaskState.OPENED -> {
                    if (hasContent) InputMenuItem.SEND_ENABLED else InputMenuItem.SEND_DISABLED
                }
                InputTaskState.SENDING -> InputMenuItem.SEND_DISABLED
                InputTaskState.FAILED -> InputMenuItem.RETRY_ENABLED
                InputTaskState.SUCCEEDED,
                InputTaskState.CANCELED -> InputMenuItem.SEND_DISABLED
            }
            emit(menu)
        }.asLiveDataWithMain(executor)

    internal val user: AppViewState<User?> = oauthRepository.getCurrentUserIdFlow()
        .flatMapLatest { id ->
            userRepository.getUserFlow(id)
                .mapLatest { it ?: userRepository.getUser(id) }
        }
        .flowOn(executor.ioContext)
        .asLiveDataWithMain(executor)

    internal val chooserForCameraApp: AppAction<CameraApp.State> = actions.cameraApp
        .scan<CameraApp.State>(CameraApp.State.Idling) { state, event -> state.transition(event) }
        .distinctUntilChanged()

    private val disposable = CompositeDisposable(
        actions.openInput.subscribeToUpdate(sharedState) {
            taskStateSource.value = InputTaskState.OPENED
        },
        actions.reply.suspendMap(executor.mainContext) {
            createReplyText(it.tweetId)
        }.subscribeToUpdate(sharedState) {
            if (it.isSuccess) {
                textSource.value = checkNotNull(it.value)
                taskStateSource.value = InputTaskState.OPENED
            }
        },
        actions.updateText.subscribeToUpdate(sharedState) {
            textSource.value = it.text
        },
        actions.updateMedia.subscribeToUpdate(sharedState) {
            when (val result = it.result) {
                is MediaChooserResult.Replace -> mediaSource.value = result.paths
                is MediaChooserResult.Add -> {
                    mediaSource.value = mediaSource.value + result.paths
                }
                is MediaChooserResult.Canceled -> Unit
            }
        },
        actions.cancelInput.subscribeToUpdate(sharedState) {
            taskStateSource.value = InputTaskState.CANCELED
            clearContents()
            taskStateSource.value = idlingState
        },
        actions.sendTweet.suspendMap(executor.mainContext) { event ->
            sharedState.taskStateSource.value = InputTaskState.SENDING
            val mediaIds = event.media.map { repository.uploadMedia(it) }
            repository.post(event.text, mediaIds)
        }.subscribeToUpdate(sharedState) {
            when {
                it.isSuccess -> {
                    taskStateSource.value = InputTaskState.SUCCEEDED
                    clearContents()
                    taskStateSource.value = idlingState
                }
                it.exception is AppTwitterException -> {
                    taskStateSource.value = InputTaskState.FAILED
                }
            }
        }
    )

    private suspend fun createReplyText(tweetId: TweetId): String {
        val item = checkNotNull(tweetRepository.findTweetListItem(tweetId))
        val targetUsers = listOfNotNull(
            if (item.isRetweet) item.originalUser.id to item.originalUser.screenName else null,
            item.body.user.id to item.body.user.screenName,
        )
        val replyEntity = replyRepository.findEntitiesByTweetId(tweetId)
            .map { it.userId to it.screenName }
        val currentUser = checkNotNull(user.value).id
        return (replyEntity + targetUsers)
            .filter { (id, _) -> id != currentUser }
            .toSet()
            .joinToString(prefix = "@", separator = " @", postfix = " ") { (_, name) -> name }
    }

    internal val taskState: AppViewState<InputTaskState> = sharedState.taskStateSource
        .filterNotNull().asLiveDataWithMain(executor)
    internal val isExpanded: AppViewState<Boolean> = sharedState.isExpanded
    internal val text: AppViewState<String> = sharedState.textSource.asLiveDataWithMain(executor)
    internal val media: AppViewState<List<AppFilePath>> = sharedState.mediaSource
        .asLiveDataWithMain(executor)

    private fun TweetInputSharedState.clearContents() {
        textSource.value = ""
        mediaSource.value = emptyList()
    }

    internal fun clear() {
        disposable.clear()
    }
}

private fun <T> Flow<T>.asLiveDataWithMain(executor: AppExecutor): AppViewState<T> {
    return asLiveData(executor.mainContext)
}
