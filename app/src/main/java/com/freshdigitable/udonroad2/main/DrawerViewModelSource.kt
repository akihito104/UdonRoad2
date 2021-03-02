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

import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.main.DrawerViewState.Companion.toClosedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.oauth.LoginUseCase
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.Serializable
import java.util.SortedSet
import javax.inject.Inject

internal data class DrawerViewState(
    val isOpened: Boolean = false,
    val isAccountSwitcherOpened: Boolean = false,
    val currentUser: TweetUserItem? = null,
    val switchableAccounts: SortedSet<TweetUserItem> = sortedSetOf()
) : ViewState, Serializable {
    companion object {
        fun DrawerViewState.toClosedState(): DrawerViewState =
            copy(isOpened = false, isAccountSwitcherOpened = false)
    }
}

internal sealed class DrawerEvent : AppEvent {
    object Opened : DrawerEvent()
    object Closed : DrawerEvent()
    object AccountSwitchClicked : DrawerEvent()
    object HomeClicked : DrawerEvent()
    object AddUserClicked : DrawerEvent()
    object CustomTimelineClicked : DrawerEvent()
    data class SwitchableAccountClicked(val accountName: String) : DrawerEvent()
    object CurrentUserIconClicked : DrawerEvent()
}

interface DrawerActionListener {
    fun onBackPressed(): Boolean
    fun onAccountSwitcherClicked()
    fun onDrawerOpened()
    fun onDrawerClosed()
    fun onDrawerMenuItemClicked(groupId: Int, itemId: Int, title: CharSequence): Boolean
    fun onCurrentUserIconClicked()
}

@ActivityScope
internal class DrawerActions @Inject constructor(
    private val dispatcher: EventDispatcher,
) : DrawerActionListener {
    override fun onBackPressed(): Boolean {
        dispatcher.postEvent(DrawerEvent.Closed)
        return true
    }

    override fun onAccountSwitcherClicked() {
        dispatcher.postEvent(DrawerEvent.AccountSwitchClicked)
    }

    override fun onDrawerOpened() {
        dispatcher.postEvent(DrawerEvent.Opened)
    }

    override fun onDrawerClosed() {
        dispatcher.postEvent(DrawerEvent.Closed)
    }

    override fun onDrawerMenuItemClicked(
        groupId: Int,
        itemId: Int,
        title: CharSequence,
    ): Boolean {
        val event = when (itemId) {
//            R.id.menu_item_drawer_home -> MainActivityEvent.DrawerEvent.HomeClicked
            R.id.menu_item_drawer_add_account -> DrawerEvent.AddUserClicked
            R.id.menu_item_drawer_lists -> DrawerEvent.CustomTimelineClicked
            else -> {
                if (groupId == R.id.menu_group_drawer_switchable_accounts) {
                    DrawerEvent.SwitchableAccountClicked(title.toString())
                } else {
                    null
                }
            }
        }
        event?.let { dispatcher.postEvent(it) }
        return event != null
    }

    override fun onCurrentUserIconClicked() {
        dispatcher.postEvent(DrawerEvent.CurrentUserIconClicked)
    }

    internal val showDrawerMenu = dispatcher.toActionFlow<DrawerEvent.Opened>()
    internal val hideDrawerMenu = dispatcher.toActionFlow<DrawerEvent.Closed>()
    internal val toggleAccountSwitcher = dispatcher.toActionFlow<DrawerEvent.AccountSwitchClicked>()
    internal val popToHome = dispatcher.toActionFlow<DrawerEvent.HomeClicked>()
    internal val launchOAuth = dispatcher.toActionFlow<DrawerEvent.AddUserClicked>()
    internal val launchCustomTimelineList =
        dispatcher.toActionFlow<DrawerEvent.CustomTimelineClicked>()
    internal val switchAccount = dispatcher.toActionFlow<DrawerEvent.SwitchableAccountClicked>()
    internal val showCurrentUser = dispatcher.toActionFlow<DrawerEvent.CurrentUserIconClicked>()
}

@ActivityScope
internal class DrawerViewModelSource @Inject constructor(
    actions: DrawerActions,
    login: LoginUseCase,
    private val listOwnerGenerator: ListOwnerGenerator,
    appSettingRepository: AppSettingRepository,
    userRepository: UserDataSource,
) : DrawerActionListener by actions {
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

    internal val state: Flow<DrawerViewState> = stateSourceBuilder(
        init = DrawerViewState(),
        currentUser.onEvent { state, user -> state.copy(currentUser = user) },
        switchableRegisteredUsers.onEvent { state, account ->
            state.copy(switchableAccounts = account)
        },
        actions.showDrawerMenu.onEvent { state, _ -> state.copy(isOpened = true) },
        actions.hideDrawerMenu.onEvent { state, _ -> state.toClosedState() },

        actions.popToHome.onEvent { state, _ -> state.toClosedState() }, // TODO
        actions.launchCustomTimelineList.onEvent { state, _ ->
            val userId = requireNotNull(state.currentUser).id
            navEventChannel.sendTimelineEvent(
                QueryType.CustomTimelineListQueryType.Ownership(userId),
                NavigationEvent.Type.NAVIGATE
            )
            state.toClosedState()
        },

        actions.showCurrentUser.onEvent { state, _ ->
            state.currentUser?.let {
                navEventChannel.send(TimelineEvent.Navigate.UserInfo(it))
            }
            state
        },
        actions.toggleAccountSwitcher.onEvent { state, _ ->
            state.copy(isAccountSwitcherOpened = !state.isAccountSwitcherOpened)
        },
        actions.launchOAuth.onEvent { state, _ ->
            navEventChannel.sendTimelineEvent(QueryType.Oauth, NavigationEvent.Type.NAVIGATE)
            state.toClosedState()
        },
        actions.switchAccount.onEvent { state, event ->
            val user =
                requireNotNull(state.switchableAccounts.find { it.account == event.accountName })
            login(user.id)
            navEventChannel.sendTimelineEvent(
                QueryType.TweetQueryType.Timeline(),
                NavigationEvent.Type.INIT
            )
            state.toClosedState()
        },
    )

    private suspend fun Channel<NavigationEvent>.sendTimelineEvent(
        queryType: QueryType,
        navType: NavigationEvent.Type
    ) {
        val timelineEvent = listOwnerGenerator.getTimelineEvent(queryType, navType)
        send(timelineEvent)
    }
}
