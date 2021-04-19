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
import com.freshdigitable.udonroad2.model.app.AppTwitterException.ErrorType
import com.freshdigitable.udonroad2.model.app.LoadingResult
import com.freshdigitable.udonroad2.model.app.load
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge

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
            when (val state = tweetRepository.load { updateLike(event.tweetId, true) }) {
                is LoadingResult.Loaded -> TweetFeedbackMessage.FAV_CREATE_SUCCESS
                is LoadingResult.Failed -> {
                    when (state.errorType) {
                        ErrorType.ALREADY_FAVORITED -> TweetFeedbackMessage.ALREADY_FAV
                        else -> TweetFeedbackMessage.FAV_CREATE_FAILURE
                    }
                }
                else -> throw IllegalStateException()
            }
        },
        actions.retweet.mapLatest { event ->
            when (val state = tweetRepository.load { updateRetweet(event.tweetId, true) }) {
                is LoadingResult.Loaded -> TweetFeedbackMessage.RT_CREATE_SUCCESS
                is LoadingResult.Failed -> {
                    when (state.errorType) {
                        ErrorType.ALREADY_RETWEETED -> TweetFeedbackMessage.ALREADY_RT
                        else -> TweetFeedbackMessage.RT_CREATE_FAILURE
                    }
                }
                else -> throw IllegalStateException()
            }
        }
    )
}
