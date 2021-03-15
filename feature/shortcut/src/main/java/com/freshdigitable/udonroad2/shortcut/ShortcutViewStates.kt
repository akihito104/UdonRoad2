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
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.AppTwitterException.ErrorType
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import java.io.IOException

interface ShortcutViewStates {
    val updateTweet: Flow<FeedbackMessage>

    companion object {
        fun create(
            actions: ShortcutActions,
            tweetRepository: TweetRepository,
        ): ShortcutViewStates = ShortcutViewStateImpl(actions, tweetRepository)
    }
}

private class ShortcutViewStateImpl(
    actions: ShortcutActions,
    tweetRepository: TweetRepository,
) : ShortcutViewStates {
    override val updateTweet: Flow<FeedbackMessage> = merge(
        actions.favTweet.mapLatest { event ->
            tweetRepository.runCatching { updateLike(event.tweetId, true) }
                .fold(
                    onSuccess = { TweetFeedbackMessage.FAV_CREATE_SUCCESS },
                    onFailure = {
                        when {
                            it.isTwitterExceptionOf(ErrorType.ALREADY_FAVORITED) ->
                                TweetFeedbackMessage.ALREADY_FAV
                            it is IOException -> TweetFeedbackMessage.FAV_CREATE_FAILURE
                            else -> throw it
                        }
                    }
                )
        },
        actions.retweet.mapLatest { event ->
            tweetRepository.runCatching { updateRetweet(event.tweetId, true) }
                .fold(
                    onSuccess = { TweetFeedbackMessage.RT_CREATE_SUCCESS },
                    onFailure = {
                        when {
                            it.isTwitterExceptionOf(ErrorType.ALREADY_RETWEETED) -> {
                                TweetFeedbackMessage.ALREADY_RT
                            }
                            it is IOException -> TweetFeedbackMessage.RT_CREATE_FAILURE
                            else -> throw it
                        }
                    }
                )
        }
    )
}

fun Throwable.isTwitterExceptionOf(type: ErrorType? = null): Boolean {
    return when (this) {
        is AppTwitterException -> {
            when (type) {
                null -> true
                else -> this.errorType == type
            }
        }
        else -> false
    }
}
