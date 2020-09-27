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

package com.freshdigitable.udonroad2.timeline

import androidx.annotation.StringRes
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventResult
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDelegate
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.StateHolder
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class TimelineViewState(
    owner: ListOwner<*>,
    actions: TimelineActions,
    selectedItemRepository: SelectedItemRepository,
    tweetRepository: TweetRepository,
    listOwnerGenerator: ListOwnerGenerator,
    private val navDelegate: TimelineNavigationDelegate,
    executor: AppExecutor,
) {
    private val _selectedItemId: AppViewState<StateHolder<SelectedItemId>> = AppAction.merge(
        AppAction.just(owner).map {
            StateHolder(selectedItemRepository.find(it))
        },
        actions.selectItem
            .filter { owner == it.owner }
            .map {
                selectedItemRepository.put(it.selectedItemId)
                StateHolder(selectedItemRepository.find(it.owner))
            },
        actions.unselectItem
            .filter { owner == it.owner }
            .map {
                selectedItemRepository.remove(it.owner)
                StateHolder(null)
            },
        actions.toggleItem
            .filter { owner == it.owner }
            .map {
                val current = selectedItemRepository.find(it.item.owner)
                when (it.item) {
                    current -> selectedItemRepository.remove(it.item.owner)
                    else -> selectedItemRepository.put(it.item)
                }
                StateHolder(selectedItemRepository.find(it.owner))
            }
    ).toViewState()

    val selectedItemId: AppViewState<SelectedItemId?> = _selectedItemId.map { it.value }

    private val updateTweet: AppAction<TimelineFeedbackMessage> = AppAction.merge(
        actions.favTweet.suspendMap(executor.dispatcher.ioContext) { event ->
            tweetRepository.postLike(event.tweetId)
        }.map {
            when {
                it.isSuccess -> TimelineFeedbackMessage.FAV_CREATE_SUCCESS
                it.isExceptionTypeOf(AppTwitterException.ErrorType.ALREADY_FAVORITED) -> {
                    TimelineFeedbackMessage.ALREADY_FAV
                }
                else -> TimelineFeedbackMessage.FAV_CREATE_FAILURE
            }
        },
        actions.retweet.suspendMap(executor.dispatcher.ioContext) { event ->
            tweetRepository.postRetweet(event.tweetId)
        }.map {
            when {
                it.isSuccess -> TimelineFeedbackMessage.RT_CREATE_SUCCESS
                it.isExceptionTypeOf(AppTwitterException.ErrorType.ALREADY_RETWEETED) -> {
                    TimelineFeedbackMessage.ALREADY_RT
                }
                else -> TimelineFeedbackMessage.RT_CREATE_FAILURE
            }
        }
    )

    private val updateNavHost: AppAction<out TimelineEvent.Navigate> = AppAction.merge(
        actions.showTimeline.map {
            TimelineEvent.Navigate.Timeline(
                listOwnerGenerator.create(QueryType.TweetQueryType.Timeline()),
                NavigationEvent.Type.INIT
            )
        },
        actions.showTweetDetail.map { TimelineEvent.Navigate.Detail(it.tweetId) },
        actions.launchUserInfo.map { TimelineEvent.Navigate.UserInfo(it) },
        actions.launchMediaViewer.filter { it.selectedItemId?.owner == owner }
            .map { TimelineEvent.Navigate.MediaViewer(it) }
    )

    private val disposables = CompositeDisposable(
        updateNavHost.subscribe { navDelegate.dispatchNavHostNavigate(it) },
        updateTweet.subscribe { navDelegate.dispatchFeedbackMessage(it) },
    )

    fun clear() {
        disposables.clear()
        navDelegate.clear()
    }
}

internal enum class TimelineFeedbackMessage(
    @StringRes override val messageRes: Int
) : FeedbackMessage {
    FAV_CREATE_SUCCESS(R.string.msg_fav_create_success),
    FAV_CREATE_FAILURE(R.string.msg_fav_create_failure),
    ALREADY_FAV(R.string.msg_already_fav),

    RT_CREATE_SUCCESS(R.string.msg_rt_create_success),
    RT_CREATE_FAILURE(R.string.msg_rt_create_failure),
    ALREADY_RT(R.string.msg_already_rt),
}

fun EventResult<*, *>.isExceptionTypeOf(type: AppTwitterException.ErrorType): Boolean {
    return (this.exception as? AppTwitterException)?.errorType == type
}

class TimelineNavigationDelegate(
    activityEventDelegate: ActivityEventDelegate,
) : NavigationDelegate, ActivityEventDelegate by activityEventDelegate
