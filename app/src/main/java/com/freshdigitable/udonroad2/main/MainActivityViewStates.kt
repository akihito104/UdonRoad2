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

import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
internal class MainActivityViewStates @Inject constructor(
    actions: MainActivityActions,
    selectedItemRepository: SelectedItemRepository,
    appSettingRepository: AppSettingRepository,
    tweetInputSharedState: TweetInputSharedState,
    listOwnerGenerator: ListOwnerGenerator,
    navDelegate: MainActivityNavState,
) {
    internal val initContainer: Flow<NavigationEvent> = AppAction.merge(
        actions.showFirstView.map {
            Timber.tag("MainActivityViewState").d("initContainer.showFirstView: $it")
            when {
                appSettingRepository.currentUserId != null -> QueryType.TweetQueryType.Timeline()
                else -> QueryType.Oauth
            }
        },
        actions.showAuth.map { QueryType.Oauth },
    ).asFlow().mapLatest {
        listOwnerGenerator.getTimelineEvent(it, NavigationEvent.Type.INIT)
    }
    internal val navigateToUser: Flow<NavigationEvent> =
        actions.showCurrentUser.asFlow().mapLatest {
            TimelineEvent.Navigate.UserInfo(it.user)
        }

    internal val states: Flow<MainActivityViewState> = stateSourceBuilder(
        init = MainActivityViewState(),
        navDelegate.containerState.onEvent { state, container ->
            state.copy(
                navHostState = container,
                selectedItem = when (container) {
                    is MainNavHostState.Timeline -> selectedItemRepository.find(container.owner)
                    else -> null
                }
            )
        },
        navDelegate.containerState.flatMapLatest {
            when (it) {
                is MainNavHostState.Timeline -> selectedItemRepository.findSource(it.owner)
                else -> emptyFlow()
            }
        }.onEvent { state, item ->
            state.copy(selectedItem = item)
        },
        tweetInputSharedState.isExpanded.onEvent { state, expanded ->
            state.copy(isTweetInputExpanded = expanded)
        },
        navDelegate.isInTopLevelDest.onEvent { state, isInTopLevel ->
            state.copy(isInTopLevelDestination = isInTopLevel)
        }
    )
}

internal data class MainActivityViewState(
    val isTweetInputExpanded: Boolean = false,
    val isInTopLevelDestination: Boolean = false,
    val navHostState: MainNavHostState? = null,
    val selectedItem: SelectedItemId? = null,
) : ViewState, Serializable {
    val isTweetInputMenuVisible: Boolean
        get() = !(navHostState is MainNavHostState.Timeline && navHostState.owner.query is QueryType.Oauth)
    val isShortcutVisible: Boolean
        get() = when {
            isTweetInputExpanded -> false
            selectedItem != null -> true
            else -> false
        }
    val appBarTitle: AppBarTitle
        get() = when (isTweetInputExpanded) {
            true -> {
                { it.getString(R.string.title_input_send_tweet) }
            }
            else -> navHostState?.appBarTitle ?: { "" }
        }
    val navIconType: NavigationIconType
        get() = when {
            isTweetInputExpanded -> NavigationIconType.CLOSE
            !isInTopLevelDestination -> NavigationIconType.UP
            else -> NavigationIconType.MENU
        }
}
