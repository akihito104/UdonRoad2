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
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.StateHolder
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.viewmodel.FragmentContainerViewStateModel
import timber.log.Timber
import javax.inject.Inject

@ActivityScope
class MainActivityStateModel @Inject constructor(
    action: MainActivityAction,
    tokenRepository: OAuthTokenRepository,
    selectedItemRepository: SelectedItemRepository
) : FragmentContainerViewStateModel {
    private val firstContainerState: AppAction<out MainNavHostState> = action.showFirstView.map {
        Timber.tag("StateModel").d("firstContainerState: $it")
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

    private val containerAction: AppAction<MainNavHostState> = AppAction.merge(
        firstContainerState,
        action.showTimeline.map {
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
        action.showTweetDetail.map { MainNavHostState.TweetDetail(it.tweetId) },
        action.popUp.map { it.state as MainNavHostState }
    )

    val containerState: AppViewState<out MainNavHostState> = containerAction.toViewState()
    private val selectedItemHolder: AppViewState<StateHolder<SelectedItemId>> = AppAction.merge(
        action.changeItemSelectState.map {
            selectedItemRepository.put(it.selectedItemId)
            StateHolder(selectedItemRepository.find(it.selectedItemId.owner))
        },
        action.toggleSelectedItem.map {
            val current = selectedItemRepository.find(it.item.owner)
            when (it.item) {
                current -> selectedItemRepository.remove(it.item.owner)
                else -> selectedItemRepository.put(it.item)
            }
            StateHolder(selectedItemRepository.find(it.item.owner))
        },
        containerAction.map {
            if (it is MainNavHostState.Timeline) {
                StateHolder(selectedItemRepository.find(it.owner))
            } else {
                StateHolder(null)
            }
        }
    ).toViewState()

    override val selectedItemId: LiveData<SelectedItemId?> = selectedItemHolder.map { it.value }

    val isFabVisible: AppViewState<Boolean> = selectedItemId.map { item -> item != null }

    val current: MainActivityViewState?
        get() {
            return if (containerState.value == null) {
                null
            } else {
                MainActivityViewState(
                    containerState = containerState.value!!,
                    selectedItem = selectedItemId.value,
                    fabVisible = isFabVisible.value ?: false
                )
            }
        }
}

data class MainActivityViewState(
    val selectedItem: SelectedItemId?,
    val fabVisible: Boolean,
    override val containerState: MainNavHostState
) : ViewState
