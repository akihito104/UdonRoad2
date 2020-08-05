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

import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.filterByType
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import timber.log.Timber
import javax.inject.Inject

@ActivityScope
class MainActivityAction @Inject constructor(
    val dispatcher: NavigationDispatcher,
    tokenRepository: OAuthTokenRepository
) {
    private val showFirstView: AppAction<out MainNavHostState> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.Setup>(),
            filterByType<OauthEvent.OauthSucceeded>().map { TimelineEvent.Setup() }
        ).map {
            Timber.tag("Action").d("showFirstView: $it")
            when {
                tokenRepository.getCurrentUserId() != null -> {
                    tokenRepository.login()
                    MainNavHostState.Timeline(
                        ListItemFragment.listOwner(QueryType.TweetQueryType.Timeline()),
                        MainNavHostState.Cause.INIT
                    )
                }
                else -> MainNavHostState.Timeline(
                    ListItemFragment.listOwner(QueryType.Oauth),
                    MainNavHostState.Cause.INIT
                )
            }
        }
    }

    val authApp: AppAction<OauthEvent.OauthRequested> = dispatcher.toAction {
        filterByType<OauthEvent.OauthRequested>()
    }

    private val showTimeline: AppAction<NavigationEvent> = dispatcher.toAction {
        AppAction.merge(
            filterByType<OauthEvent.Init>(),
            filterByType<TimelineEvent.Init>()
        )
    }

    private val backDispatched: AppAction<NavigationEvent> = dispatcher.emitter
        .filterByType<CommonEvent.Back>()
        .map {
            val currentState = it.currentState as? MainActivityViewState ?: return@map it
            when {
                currentState.selectedItem != null -> {
                    TimelineEvent.TweetItemSelected(
                        SelectedItemId(currentState.selectedItem.owner, null)
                    )
                }
                else -> it
            }
        }

    val launchMediaViewer: AppAction<TimelineEvent.MediaItemClicked> = dispatcher.toAction {
        filterByType<TimelineEvent.MediaItemClicked>()
    }

    val changeItemSelectState: AppAction<TimelineEvent.TweetItemSelected> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.TweetItemSelected>(),
            backDispatched.filterByType<TimelineEvent.TweetItemSelected>(),
            launchMediaViewer
                .filter { it.selectedItemId != null }
                .map { TimelineEvent.TweetItemSelected(requireNotNull(it.selectedItemId)) }
        )
    }
    val toggleSelectedItem: AppAction<TimelineEvent.ToggleTweetItemSelectedState> =
        dispatcher.toAction {
            filterByType<TimelineEvent.ToggleTweetItemSelectedState>()
        }

    private val showTweetDetail: AppAction<TimelineEvent.TweetDetailRequested> =
        dispatcher.toAction {
            filterByType<TimelineEvent.TweetDetailRequested>()
        }

    val rollbackViewState: AppAction<CommonEvent.Back> = backDispatched.filterByType()

    val updateContainer: AppAction<MainNavHostState> = AppAction.merge(
        showFirstView,
        showTimeline.map {
            when (it) {
                is OauthEvent.Init -> MainNavHostState.Timeline(
                    ListItemFragment.listOwner(QueryType.Oauth),
                    MainNavHostState.Cause.INIT
                )
                is TimelineEvent.Init -> MainNavHostState.Timeline(
                    ListItemFragment.listOwner(QueryType.TweetQueryType.Timeline()),
                    MainNavHostState.Cause.INIT
                )
                else -> TODO("not implemented")
            }
        },
        showTweetDetail.map { MainNavHostState.TweetDetail(it.tweetId) },
        dispatcher.emitter.filterByType<TimelineEvent.DestinationChanged>().map {
            it.state as MainNavHostState
        }
    )

    val launchUserInfo: AppAction<TweetingUser> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.UserIconClicked>().map { it.user },
            filterByType<TimelineEvent.RetweetUserClicked>().map { it.user }
        )
    }
}
