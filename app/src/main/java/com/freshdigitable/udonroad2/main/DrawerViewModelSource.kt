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

import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.main.DrawerViewModelSource.DrawerViewState.Companion.toClosedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.model.app.navigation.getTimelineEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.oauth.LoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.SortedSet
import javax.inject.Inject

interface DrawerViewModel : DrawerEventListener {
    val drawerState: LiveData<State>

    interface State : ViewState {
        val isOpened: Boolean
        val isAccountSwitcherOpened: Boolean
        val currentUser: TweetUserItem?
        val switchableAccounts: SortedSet<TweetUserItem>
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

interface DrawerEventListener {
    val showDrawerMenu: AppEventListener
    val hideDrawerMenu: AppEventListener
    val showCurrentUser: AppEventListener
    val toggleAccountSwitcher: AppEventListener
    fun onDrawerMenuItemClicked(groupId: Int, itemId: Int, title: CharSequence): Boolean
}

@ActivityScope
internal class DrawerActions @Inject constructor(
    dispatcher: EventDispatcher,
) : DrawerEventListener {
    override val showDrawerMenu = dispatcher.toAction(DrawerEvent.Opened)
    override val hideDrawerMenu = dispatcher.toAction(DrawerEvent.Closed)
    override val showCurrentUser = dispatcher.toAction(DrawerEvent.CurrentUserIconClicked)
    override val toggleAccountSwitcher = dispatcher.toAction(DrawerEvent.AccountSwitchClicked)

    internal val popToHome = dispatcher.toAction(DrawerEvent.HomeClicked)
    internal val launchOAuth = dispatcher.toAction(DrawerEvent.AddUserClicked)
    internal val launchCustomTimelineList = dispatcher.toAction(DrawerEvent.CustomTimelineClicked)
    internal val switchAccount = dispatcher.toAction { name: String ->
        DrawerEvent.SwitchableAccountClicked(name)
    }

    override fun onDrawerMenuItemClicked(
        groupId: Int,
        itemId: Int,
        title: CharSequence,
    ): Boolean {
        when (itemId) {
//            R.id.menu_item_drawer_home -> MainActivityEvent.DrawerEvent.HomeClicked
            R.id.menu_item_drawer_add_account -> launchOAuth.dispatch()
            R.id.menu_item_drawer_lists -> launchCustomTimelineList.dispatch()
            else -> {
                if (groupId == R.id.menu_group_drawer_switchable_accounts) {
                    switchAccount.dispatch(title.toString())
                } else {
                    return false
                }
            }
        }
        return true
    }
}

@ActivityScope
internal class DrawerViewModelSource @Inject constructor(
    actions: DrawerActions,
    login: LoginUseCase,
    private val listOwnerGenerator: ListOwnerGenerator,
    appSettingRepository: AppSettingRepository,
    userRepository: UserDataSource,
) : DrawerEventListener by actions {
    private val navEventChannel: Channel<AppEffect.Navigation> = Channel()
    internal val navEventSource: Flow<AppEffect.Navigation> = navEventChannel.receiveAsFlow()

    internal val state: Flow<DrawerViewModel.State> = stateSourceBuilder(
        init = DrawerViewState(),
        actions.showDrawerMenu.onEvent { state, _ -> state.copy(isOpened = true) },
        actions.hideDrawerMenu.onEvent { state, _ -> state.toClosedState() },

        actions.popToHome.onEvent { state, _ -> state.toClosedState() }, // TODO
        actions.launchCustomTimelineList.onEvent { state, _ ->
            val userId = requireNotNull(state.currentUser).id
            navEventChannel.sendTimelineEvent(
                QueryType.CustomTimelineListQueryType.Ownership(userId),
            )
            state.toClosedState()
        },

        actions.showCurrentUser.onEvent { state, _ ->
            state.currentUser?.let {
                navEventChannel.send(TimelineEffect.Navigate.UserInfo(it))
            }
            state
        },
        actions.toggleAccountSwitcher.onEvent { state, _ ->
            state.copy(isAccountSwitcherOpened = !state.isAccountSwitcherOpened)
        },
        actions.launchOAuth.onEvent { state, _ ->
            navEventChannel.sendTimelineEvent(QueryType.Oauth)
            state.toClosedState()
        },
        actions.switchAccount.onEvent { state, event ->
            val user =
                requireNotNull(state.switchableAccounts.find { it.account == event.accountName })
            login(user.id)
            navEventChannel.sendTimelineEvent(
                QueryType.TweetQueryType.Timeline(),
                AppEffect.Navigation.Type.INIT
            )
            state.toClosedState()
        },
        appSettingRepository.currentUserIdSource.flatMapLatest { id ->
            userRepository.getUserSource(id).mapLatest { id to it }
        }.onEvent { s, (id: UserId, user: UserEntity?) ->
            val switchableAccounts = when {
                s.currentUserId != id ->
                    userRepository.getSwitchableUsers(s, id, s.registeredUserId)
                else -> s.switchableAccounts
            }
            s.copy(currentUserId = id, currentUser = user, switchableAccounts = switchableAccounts)
        },
        appSettingRepository.registeredUserIdsSource.onEvent { s, ids ->
            val switchableAccounts = userRepository.getSwitchableUsers(s, s.currentUserId, ids)
            s.copy(registeredUserId = ids, switchableAccounts = switchableAccounts)
        },
    )

    private suspend fun Channel<AppEffect.Navigation>.sendTimelineEvent(
        queryType: QueryType,
        navType: AppEffect.Navigation.Type = AppEffect.Navigation.Type.NAVIGATE,
    ) {
        val timelineEvent = listOwnerGenerator.getTimelineEvent(queryType, navType)
        send(timelineEvent)
    }

    internal data class DrawerViewState(
        override val isOpened: Boolean = false,
        override val isAccountSwitcherOpened: Boolean = false,
        internal val currentUserId: UserId? = null,
        override val currentUser: TweetUserItem? = null,
        internal val registeredUserId: Set<UserId> = emptySet(),
        override val switchableAccounts: SortedSet<TweetUserItem> = sortedSetOf(),
    ) : DrawerViewModel.State {
        internal val switchableUserIds: Set<UserId>
            get() = switchableUserIds(currentUserId, registeredUserId)

        companion object {
            fun DrawerViewState.toClosedState(): DrawerViewState =
                copy(isOpened = false, isAccountSwitcherOpened = false)

            fun switchableUserIds(
                currentUserId: UserId?,
                registeredUserId: Set<UserId>,
            ): Set<UserId> = currentUserId?.let { registeredUserId - it } ?: emptySet()
        }
    }

    companion object {
        private val sortWithScreenName: Comparator<TweetUserItem> = Comparator { o1, o2 ->
            requireNotNull(o1)
            requireNotNull(o2)
            o1.screenName.compareTo(o2.screenName)
        }

        private suspend fun UserDataSource.getSwitchableUsers(
            state: DrawerViewState,
            currentUserId: UserId?,
            registeredUserId: Set<UserId>,
        ): SortedSet<TweetUserItem> {
            val switchableUserIds =
                DrawerViewState.switchableUserIds(currentUserId, registeredUserId)
            return if (switchableUserIds != state.switchableUserIds) {
                switchableUserIds.map { getUser(it) }.toSortedSet(sortWithScreenName)
            } else {
                sortedSetOf()
            }
        }
    }
}
