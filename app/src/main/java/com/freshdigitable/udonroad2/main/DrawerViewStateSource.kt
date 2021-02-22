/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.main.MainActivityEvent.DrawerEvent
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.ScanFun
import com.freshdigitable.udonroad2.model.app.UpdateFun
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.oauth.LoginUseCase
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.rx2.asFlow
import java.io.Serializable
import java.util.SortedSet
import javax.inject.Inject

internal data class DrawerViewState(
    val isOpened: Boolean = false,
    val isAccountSwitcherOpened: Boolean = false,
    val currentUser: TweetUserItem? = null,
    val switchableAccounts: SortedSet<TweetUserItem> = sortedSetOf()
) : ViewState, Serializable

internal class DrawerActions @Inject constructor(
    dispatcher: EventDispatcher,
) {
    internal val showDrawerMenu = dispatcher.toAction<DrawerEvent.Opened>().asFlow()
    internal val hideDrawerMenu = dispatcher.toAction<DrawerEvent.Closed>().asFlow()
    internal val toggleAccountSwitcher = dispatcher.toAction<DrawerEvent.AccountSwitchClicked>()
        .asFlow()
    internal val popToHome = dispatcher.toAction<DrawerEvent.HomeClicked>().asFlow()
    internal val launchOAuth = dispatcher.toAction<DrawerEvent.AddUserClicked>().asFlow()
    internal val launchCustomTimelineList =
        dispatcher.toAction<DrawerEvent.CustomTimelineClicked>().asFlow()
    internal val switchAccount =
        dispatcher.toAction<DrawerEvent.SwitchableAccountClicked>().asFlow()
}

internal class DrawerViewStateSource @Inject constructor(
    actions: DrawerActions,
    login: LoginUseCase,
    listOwnerGenerator: ListOwnerGenerator,
    appSettingRepository: AppSettingRepository,
    userRepository: UserDataSource,
) {
    private val navEventChannel: Channel<NavigationEvent> = Channel()
    internal val navEventSource: Flow<NavigationEvent> = navEventChannel.receiveAsFlow()

    private val currentUser: Flow<TweetUserItem> = appSettingRepository.currentUserIdSource
        .flatMapLatest { userRepository.getUserSource(it) }
        .filterNotNull()
    private val switchableRegisteredUsers: Flow<SortedSet<TweetUserItem>> = combine(
        appSettingRepository.registeredUserIdsSource,
        appSettingRepository.currentUserIdSource,
    ) { registered, current ->
        (registered - current).map { userRepository.getUser(it) }
            .toSortedSet<TweetUserItem> { a, b ->
                a.screenName.compareTo(b.screenName)
            }
    }.onStart { emit(sortedSetOf()) }
    private val updateSources: Array<Flow<UpdateFun<DrawerViewState>>> = arrayOf(
        currentUser.onEvent { state, user -> state.copy(currentUser = user) },
        switchableRegisteredUsers.onEvent { state, account ->
            state.copy(switchableAccounts = account)
        },
        actions.showDrawerMenu.onEvent { state, _ -> state.copy(isOpened = true) },
        actions.hideDrawerMenu.onEvent { state, _ ->
            state.copy(isOpened = false, isAccountSwitcherOpened = false)
        },

        actions.popToHome.onEvent { acc, _ ->
            acc.copy(isOpened = false, isAccountSwitcherOpened = false)
        },
        actions.launchCustomTimelineList.onEvent { acc, _ ->
            val userId = requireNotNull(acc.currentUser).id
            val ev = listOwnerGenerator.getTimelineEvent(
                QueryType.CustomTimelineListQueryType.Ownership(userId),
                NavigationEvent.Type.NAVIGATE
            )
            navEventChannel.send(ev)
            acc.copy(isOpened = false, isAccountSwitcherOpened = false)
        },

        actions.toggleAccountSwitcher.onEvent { acc, _ ->
            acc.copy(isAccountSwitcherOpened = !acc.isAccountSwitcherOpened)
        },
        actions.launchOAuth.onEvent { acc, _ ->
            val timelineEvent = listOwnerGenerator.getTimelineEvent(
                QueryType.Oauth,
                NavigationEvent.Type.NAVIGATE
            )
            navEventChannel.send(timelineEvent)
            acc.copy(isOpened = false, isAccountSwitcherOpened = false)
        },
        actions.switchAccount.onEvent { acc, event ->
            login(event.userId)
            val timelineEvent = listOwnerGenerator.getTimelineEvent(
                QueryType.TweetQueryType.Timeline(),
                NavigationEvent.Type.INIT
            )
            navEventChannel.send(timelineEvent)
            acc.copy(isOpened = false, isAccountSwitcherOpened = false)
        },
    )

    internal val state: Flow<DrawerViewState> = merge(*updateSources)
        .scan(DrawerViewState()) { state, trans -> trans(state) }

    private inline fun <reified E> Flow<E>.onEvent(
        noinline update: ScanFun<DrawerViewState, E>
    ): Flow<UpdateFun<DrawerViewState>> = onEvent(this, update)
}