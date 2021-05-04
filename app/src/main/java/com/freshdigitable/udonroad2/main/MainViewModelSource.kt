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

import com.freshdigitable.fabshortcut.ShortcutViewHolder
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.navigation.getTimelineEvent
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.shortcut.MenuItemState
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

@ActivityScope
internal class MainActivityActions @Inject constructor(
    dispatcher: EventDispatcher,
) {
    internal val showFirstView = dispatcher.toActionFlow<TimelineEvent.Setup>()
}

@ActivityScope
internal class MainViewModelSource @Inject constructor(
    actions: MainActivityActions,
    selectedItemRepository: SelectedItemRepository,
    appSettingRepository: AppSettingRepository,
    tweetRepository: TweetRepository,
    tweetInputSharedState: TweetInputSharedState,
    listOwnerGenerator: ListOwnerGenerator,
    navDelegate: MainActivityNavState,
) {
    internal val initContainer: Flow<AppEffect.Navigation> = actions.showFirstView.mapLatest {
        val type = when {
            appSettingRepository.currentUserId != null -> QueryType.TweetQueryType.Timeline()
            else -> QueryType.Oauth
        }
        listOwnerGenerator.getTimelineEvent(type, AppEffect.Navigation.Type.INIT)
    }
    internal val states: Flow<MainActivityViewState> = stateSourceBuilder(
        init = MainActivityViewState(),
        appSettingRepository.currentUserIdSource.onEvent { s, id -> s.copy(currentUserId = id) },
        navDelegate.containerState.flatMapLatest { host ->
            when (host) {
                is MainNavHostState.Timeline -> selectedItemRepository.getSource(host.owner)
                    .mapLatest { host to it }
                else -> flowOf(host to null)
            }
        }.flatMapLatest { (container: MainNavHostState, item: SelectedItemId?) ->
            when (container) {
                is MainNavHostState.Timeline -> {
                    item?.let { i ->
                        tweetRepository.getDetailTweetItemSource(i.quoteId ?: i.originalId)
                            .mapLatest { container to it }
                    } ?: flowOf(container to null)
                }
                is MainNavHostState.TweetDetail -> {
                    tweetRepository.getDetailTweetItemSource(container.tweetId)
                        .mapLatest { container to it }
                }
                else -> flowOf(container to null)
            }
        }.onEvent { state, (container: MainNavHostState, item: DetailTweetListItem?) ->
            state.copy(navHostState = container, selectedItem = item)
        },
        tweetInputSharedState.isExpanded.onEvent { state, expanded ->
            state.copy(isTweetInputExpanded = expanded)
        },
        navDelegate.isInTopLevelDest.onEvent { state, isInTopLevel ->
            state.copy(isInTopLevelDestination = isInTopLevel)
        },
    )
}

internal data class MainActivityViewState(
    val isTweetInputExpanded: Boolean = false,
    val isInTopLevelDestination: Boolean = false,
    val navHostState: MainNavHostState? = null,
    val selectedItem: DetailTweetListItem? = null,
    private val currentUserId: UserId? = null,
) : ViewState, ShortcutViewModel.State {
    val isTweetInputMenuVisible: Boolean
        get() = (navHostState as? MainNavHostState.Timeline)?.owner?.query !is QueryType.Oauth

    override val mode: ShortcutViewHolder.Mode
        get() = when (navHostState) {
            is MainNavHostState.Timeline -> {
                if (selectedItem != null && !isTweetInputExpanded) {
                    ShortcutViewHolder.Mode.FAB
                } else {
                    ShortcutViewHolder.Mode.HIDDEN
                }
            }
            is MainNavHostState.TweetDetail -> ShortcutViewHolder.Mode.TOOLBAR
            else -> ShortcutViewHolder.Mode.HIDDEN
        }
    override val menuItemState: MenuItemState
        get() = when {
            selectedItem != null && mode == ShortcutViewHolder.Mode.TOOLBAR -> MenuItemState(
                isMainGroupEnabled = true,
                isRetweetChecked = selectedItem.body.isRetweeted,
                isFavChecked = selectedItem.body.isFavorited,
                isDeleteVisible = selectedItem.originalUser.id == currentUserId
            )
            else -> MenuItemState()
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
