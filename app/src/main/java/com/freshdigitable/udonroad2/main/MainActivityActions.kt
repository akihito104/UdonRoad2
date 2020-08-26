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
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.filterByType
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.oauth.OauthAction
import com.freshdigitable.udonroad2.timeline.TimelineActions
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

@ActivityScope
class MainActivityActions @Inject constructor(
    dispatcher: EventDispatcher,
    tokenRepository: OAuthTokenRepository,
    listOwnerGenerator: ListOwnerGenerator,
    timelineActions: TimelineActions,
    oauthAction: OauthAction
) {
    private val showFirstView: AppAction<out MainNavHostState> = dispatcher.toAction {
        AppAction.merge(
            filterByType<TimelineEvent.Setup>(),
            oauthAction.authSuccess.map { TimelineEvent.Setup() }
        ).map {
            Timber.tag("Action").d("showFirstView: $it")
            when {
                tokenRepository.getCurrentUserId() != null -> {
                    tokenRepository.login()
                    MainNavHostState.Timeline(
                        listOwnerGenerator.create(QueryType.TweetQueryType.Timeline()),
                        MainNavHostState.Cause.INIT
                    )
                }
                else -> MainNavHostState.Timeline(
                    listOwnerGenerator.create(QueryType.Oauth),
                    MainNavHostState.Cause.INIT
                )
            }
        }
    }

    val rollbackViewState: AppAction<CommonEvent.Back> = dispatcher.toAction {
        filterByType()
    }

    val updateContainer: AppAction<MainNavHostState> = AppAction.merge(
        listOf(
            showFirstView,
            oauthAction.showAuth.map {
                MainNavHostState.Timeline(
                    listOwnerGenerator.create(QueryType.Oauth),
                    MainNavHostState.Cause.INIT
                )
            },
            timelineActions.showTimeline.map {
                MainNavHostState.Timeline(
                    listOwnerGenerator.create(QueryType.TweetQueryType.Timeline()),
                    MainNavHostState.Cause.INIT
                )
            },
            timelineActions.showTweetDetail.map { MainNavHostState.TweetDetail(it.tweetId) },
        )
    )
}
