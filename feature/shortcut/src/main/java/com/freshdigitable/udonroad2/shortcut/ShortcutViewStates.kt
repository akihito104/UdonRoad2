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

package com.freshdigitable.udonroad2.shortcut

import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventResult
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap

interface ShortcutViewStates {
    val updateTweet: AppAction<FeedbackMessage>

    companion object {
        fun create(
            actions: ShortcutActions,
            tweetRepository: TweetRepository,
            executor: AppExecutor
        ): ShortcutViewStates = ShortcutViewStateImpl(actions, tweetRepository, executor)
    }
}

private class ShortcutViewStateImpl(
    actions: ShortcutActions,
    tweetRepository: TweetRepository,
    executor: AppExecutor,
) : ShortcutViewStates {
    override val updateTweet: AppAction<FeedbackMessage> = AppAction.merge(
        actions.favTweet.suspendMap(executor.mainContext) { event ->
            tweetRepository.updateLike(event.tweetId, true)
        }.map {
            when {
                it.isSuccess -> TweetFeedbackMessage.FAV_CREATE_SUCCESS
                it.isExceptionTypeOf(AppTwitterException.ErrorType.ALREADY_FAVORITED) -> {
                    TweetFeedbackMessage.ALREADY_FAV
                }
                else -> TweetFeedbackMessage.FAV_CREATE_FAILURE
            }
        },
        actions.retweet.suspendMap(executor.mainContext) { event ->
            tweetRepository.updateRetweet(event.tweetId, true)
        }.map {
            when {
                it.isSuccess -> TweetFeedbackMessage.RT_CREATE_SUCCESS
                it.isExceptionTypeOf(AppTwitterException.ErrorType.ALREADY_RETWEETED) -> {
                    TweetFeedbackMessage.ALREADY_RT
                }
                else -> TweetFeedbackMessage.RT_CREATE_FAILURE
            }
        }
    )
}

fun EventResult<*, *>.isExceptionTypeOf(type: AppTwitterException.ErrorType): Boolean {
    return (this.exception as? AppTwitterException)?.errorType == type
}
