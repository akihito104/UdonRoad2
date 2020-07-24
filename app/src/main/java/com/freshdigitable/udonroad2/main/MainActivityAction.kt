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

import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.filterByType
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import javax.inject.Inject

@ActivityScope
class MainActivityAction @Inject constructor(
    dispatcher: NavigationDispatcher
) {
    val showFirstView: AppAction<TimelineEvent.Setup> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.Setup>(),
            filterByType<OauthEvent.OauthSucceeded>().map { TimelineEvent.Setup }
        )
    }

    val authApp: AppAction<OauthEvent.OauthRequested> = dispatcher.toAction {
        filterByType<OauthEvent.OauthRequested>()
    }

    val showTimeline: AppAction<QueryType> = dispatcher.toAction {
        AppAction.merge(
            filterByType<OauthEvent.Init>().map { QueryType.Oauth },
            filterByType<TimelineEvent.Init>().map { QueryType.TweetQueryType.Timeline() }
        )
    }

    private val backDispatched: AppAction<NavigationEvent> = dispatcher.emitter
        .filterByType<CommonEvent.Back>()
        .map {
            val currentState = it.currentState as MainActivityViewState
            when {
                currentState.selectedItem != null -> {
                    TimelineEvent.TweetItemSelected(
                        SelectedItemId(currentState.selectedItem.owner, null)
                    )
                }
                else -> it
            }
        }
    val changeItemSelectState: AppAction<TimelineEvent.TweetItemSelected> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.TweetItemSelected>(),
            backDispatched.filterByType<TimelineEvent.TweetItemSelected>()
        )
    }
    val toggleSelectedItem: AppAction<TimelineEvent.ToggleTweetItemSelectedState> =
        dispatcher.toAction {
            filterByType<TimelineEvent.ToggleTweetItemSelectedState>()
        }

    val showTweetDetail: AppAction<TimelineEvent.TweetDetailRequested> = dispatcher.toAction {
        filterByType<TimelineEvent.TweetDetailRequested>()
    }

    val rollbackViewState: AppAction<CommonEvent.Back> = backDispatched.filterByType()

    val launchUserInfo: AppAction<TweetingUser> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.UserIconClicked>().map { it.user },
            filterByType<TimelineEvent.RetweetUserClicked>().map { it.user }
        )
    }

    val launchMediaViewer: AppAction<TimelineEvent.MediaItemClicked> = dispatcher.toAction {
        filterByType<TimelineEvent.MediaItemClicked>()
    }
}
