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

package com.freshdigitable.udonroad2.main

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventResult
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.timeline.TimelineActions
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
class MainActivityViewStates @Inject constructor(
    actions: MainActivityActions,
    timelineActions: TimelineActions,
    selectedItemRepository: SelectedItemRepository,
    tweetRepository: TweetRepository,
) {
    private val container: AppViewState<MainNavHostState> = actions.updateContainer.toViewState()

    val selectedItemId: AppViewState<SelectedItemId?> = container.switchMap {
        when (it) {
            is MainNavHostState.Timeline -> selectedItemRepository.observe(it.owner)
            else -> MutableLiveData(null)
        }
    }

    val isFabVisible: AppViewState<Boolean> = selectedItemId.map { it != null }

    val current: MainActivityViewState?
        get() {
            return MainActivityViewState(
                selectedItem = selectedItemId.value,
                fabVisible = isFabVisible.value ?: false
            )
        }

    val updateTweet: AppAction<FeedbackMessage> = AppAction.merge(
        timelineActions.favTweet.flatMap { event ->
            tweetRepository.postLike(event.tweetId).map { EventResult(event, it) }
        }.map {
            when {
                it.isSuccess -> FeedbackMessage.FavCreateSuccess
                it.isExceptionTypeOf(AppTwitterException.ErrorType.ALREADY_FAVORITED) -> {
                    FeedbackMessage.AlreadyFav
                }
                else -> FeedbackMessage.FavCreateFailed
            }
        },
        timelineActions.retweet.flatMap { event ->
            tweetRepository.postRetweet(event.tweetId).map { EventResult(event, it) }
        }.map {
            when {
                it.isSuccess -> FeedbackMessage.RtCreateSuccess
                it.isExceptionTypeOf(AppTwitterException.ErrorType.ALREADY_RETWEETED) -> {
                    FeedbackMessage.AlreadyRt
                }
                else -> FeedbackMessage.RtCreateFailed
            }
        }
    )
}

data class MainActivityViewState(
    val selectedItem: SelectedItemId?,
    val fabVisible: Boolean
) : ViewState, Serializable

sealed class FeedbackMessage(
    @StringRes val messageRes: Int
) {
    object FavCreateSuccess : FeedbackMessage(R.string.msg_fav_create_success)
    object FavCreateFailed : FeedbackMessage(R.string.msg_fav_create_failed)
    object AlreadyFav : FeedbackMessage(R.string.msg_already_fav)

    object RtCreateSuccess : FeedbackMessage(R.string.msg_rt_create_success)
    object RtCreateFailed : FeedbackMessage(R.string.msg_rt_create_failed)
    object AlreadyRt : FeedbackMessage(R.string.msg_already_rt)
}

fun EventResult<*>.isExceptionTypeOf(type: AppTwitterException.ErrorType): Boolean {
    return (this.exception as? AppTwitterException)?.errorType == type
}
