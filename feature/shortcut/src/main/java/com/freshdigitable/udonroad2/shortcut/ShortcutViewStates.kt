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
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.AppErrorType
import com.freshdigitable.udonroad2.model.app.AppTwitterException.ErrorType
import com.freshdigitable.udonroad2.model.app.LoadingResult
import com.freshdigitable.udonroad2.model.app.load
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.app.navigation.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class ShortcutViewStates @Inject constructor(
    actions: ShortcutActions,
    tweetRepository: TweetRepository,
    listOwnerGenerator: ListOwnerGenerator,
) : ShortcutEventListener by actions, ActivityEffectStream {
    override val effect: Flow<AppEffect> = merge(
        actions.favTweet.mapLatest { event ->
            tweetRepository.load { updateLike(event.tweetId, true) }.fold(
                loaded = TweetFeedbackMessage.FAV_CREATE_SUCCESS,
                onFailed = {
                    when (it) {
                        ErrorType.ALREADY_FAVORITED -> TweetFeedbackMessage.ALREADY_FAV
                        else -> TweetFeedbackMessage.FAV_CREATE_FAILURE
                    }
                }
            )
        },
        actions.unlikeTweet.mapLatest {
            tweetRepository.load { updateLike(it.tweetId, false) }.fold(
                loaded = TweetFeedbackMessage.FAV_DESTROY_SUCCESS,
                failed = TweetFeedbackMessage.FAV_DESTROY_FAILURE
            )
        },
        actions.retweet.mapLatest { event ->
            tweetRepository.load { updateRetweet(event.tweetId, true) }.fold(
                loaded = TweetFeedbackMessage.RT_CREATE_SUCCESS,
                onFailed = {
                    when (it) {
                        ErrorType.ALREADY_RETWEETED -> TweetFeedbackMessage.ALREADY_RT
                        else -> TweetFeedbackMessage.RT_CREATE_FAILURE
                    }
                }
            )
        },
        actions.unretweetTweet.mapLatest {
            tweetRepository.load { updateRetweet(it.tweetId, false) }.fold(
                loaded = TweetFeedbackMessage.RT_DESTROY_SUCCESS,
                failed = TweetFeedbackMessage.RT_DESTROY_FAILURE
            )
        },
        actions.deleteTweet.mapLatest {
            val detailTweetItem = tweetRepository.findDetailTweetItem(it.tweetId) // TODO
            val id = detailTweetItem?.body?.retweetIdByCurrentUser ?: it.tweetId
            tweetRepository.load { deleteTweet(id) }.fold(
                loaded = TweetFeedbackMessage.DELETE_TWEET_SUCCESS,
                failed = TweetFeedbackMessage.DELETE_TWEET_FAILURE
            )
        },
        actions.showTweetDetail.mapLatest {
            TimelineEffect.Navigate.Detail(it.tweetId)
        },
        actions.showConversation.mapLatest {
            val queryType = QueryType.TweetQueryType.Conversation(it.tweetId)
            listOwnerGenerator.getTimelineEvent(queryType)
        }
    )

    companion object {
        private fun <T, R> LoadingResult<T>.fold(
            loaded: R? = null,
            onLoaded: (T) -> R = { requireNotNull(loaded) },
            failed: R? = null,
            onFailed: (AppErrorType) -> R = { requireNotNull(failed) },
        ): R = when (this) {
            is LoadingResult.Loaded -> loaded ?: onLoaded(this.value)
            is LoadingResult.Failed -> failed ?: onFailed(this.errorType)
            else -> throw IllegalStateException()
        }
    }
}
