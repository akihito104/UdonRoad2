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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.ext.combineLatest
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
internal class MainActivityViewStates @Inject constructor(
    actions: MainActivityActions,
    selectedItemRepository: SelectedItemRepository,
    appSettingRepository: AppSettingRepository,
    private val tweetInputSharedState: TweetInputSharedState,
    listOwnerGenerator: ListOwnerGenerator,
    navDelegate: MainActivityNavState,
    userRepository: UserDataSource,
    executor: AppExecutor,
) {
    internal val currentUser: AppViewState<TweetUserItem> = appSettingRepository.currentUserIdSource
        .flatMapLatest { userRepository.getUserSource(it) }
        .filterNotNull()
        .asLiveData(executor.mainContext)
    internal val switchableRegisteredUsers: AppViewState<Set<TweetUserItem>> = combine(
        appSettingRepository.registeredUserIdsSource,
        appSettingRepository.currentUserIdSource,
    ) { registered, current ->
        (registered - current).map { userRepository.getUser(it) }.toSet()
    }.onStart { emit(emptySet()) }.asLiveData(executor.mainContext)

    internal val isRegisteredUsersOpened: AppViewState<Boolean> =
        actions.toggleAccountSwitcher.asFlow()
            .scan(false) { acc, _ -> !acc }
            .asLiveData(executor.mainContext)

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

    private val currentNavHost: AppViewState<MainNavHostState> = navDelegate.containerState
    val isTweetInputMenuVisible: LiveData<Boolean> = currentNavHost.map {
        !(it is MainNavHostState.Timeline && it.owner.query is QueryType.Oauth)
    }.distinctUntilChanged()
    val isTweetInputExpanded: Boolean
        get() = tweetInputSharedState.isExpanded.value ?: false

    val appBarTitle: AppViewState<AppBarTitle> = combineLatest(
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
    val navIconType: AppViewState<NavigationIconType> = combineLatest(
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

    val isFabVisible: AppViewState<Boolean> = combineLatest(
        selectedItemId.map { it != null },
        tweetInputSharedState.isExpanded
    ) { selected, expanded ->
        when {
            expanded == true -> false
            selected == true -> true
            else -> false
        }
    }

    val current: MainActivityViewState
        get() {
            return MainActivityViewState(
                selectedItem = selectedItemId.value,
                fabVisible = isFabVisible.value ?: false
            )
        }
}

data class MainActivityViewState(
    val selectedItem: SelectedItemId?,
    val fabVisible: Boolean
) : ViewState, Serializable
