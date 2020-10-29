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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.ext.merge
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.reactivex.disposables.CompositeDisposable
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
class MainActivityViewStates @Inject constructor(
    actions: MainActivityActions,
    selectedItemRepository: SelectedItemRepository,
    tokenRepository: OAuthTokenRepository,
    tweetInputSharedState: TweetInputSharedState,
    listOwnerGenerator: ListOwnerGenerator,
    private val navDelegate: MainActivityNavigationDelegate,
) {

    private val updateContainer: AppAction<out NavigationEvent> = AppAction.merge(
        actions.showFirstView.map {
            when {
                tokenRepository.getCurrentUserId() != null -> {
                    tokenRepository.login()
                    TimelineEvent.Navigate.Timeline(
                        listOwnerGenerator.create(QueryType.TweetQueryType.Timeline()),
                        NavigationEvent.Type.INIT
                    )
                }
                else -> TimelineEvent.Navigate.Timeline(
                    listOwnerGenerator.create(QueryType.Oauth), NavigationEvent.Type.INIT
                )
            }
        },
        actions.showAuth.map {
            TimelineEvent.Navigate.Timeline(
                listOwnerGenerator.create(QueryType.Oauth), NavigationEvent.Type.INIT
            )
        },
    )
    private val disposables = CompositeDisposable(
        updateContainer.subscribe { navDelegate.dispatchNavHostNavigate(it) },
        actions.rollbackViewState.subscribe { navDelegate.dispatchBack() },
    )

    private val currentNavHost: AppViewState<MainNavHostState> = navDelegate.containerState
    val appBarTitle: AppViewState<AppBarTitle> = merge(
        tweetInputSharedState.isExpanded,
        currentNavHost
    ) { expanded, navHost ->
        when (expanded) {
            true -> {
                { it.getString(R.string.title_input_send_tweet) }
            }
            else -> navHost?.appBarTitle ?: { "" }
        }
    }
    val navIconType: AppViewState<NavigationIconType> = merge(
        tweetInputSharedState.isExpanded,
        navDelegate.isInTopLevelDest
    ) { expanded, inTopLevel ->
        when {
            expanded == true -> NavigationIconType.CLOSE
            inTopLevel == false -> NavigationIconType.UP
            else -> NavigationIconType.MENU
        }
    }

    private val selectedItemId: AppViewState<SelectedItemId?> = currentNavHost.switchMap {
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

    fun clear() {
        disposables.clear()
        navDelegate.clear()
    }
}

data class MainActivityViewState(
    val selectedItem: SelectedItemId?,
    val fabVisible: Boolean
) : ViewState, Serializable
