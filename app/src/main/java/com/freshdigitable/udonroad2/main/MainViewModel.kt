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

import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.input.TweetInputEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.shortcut.postSelectedItemShortcutEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

internal class MainViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: MainActivityViewStates,
) : ViewModel(), ShortcutViewModel {

    val navIconType: LiveData<NavigationIconType> = viewStates.navIconType
    val appBarTitle: LiveData<AppBarTitle> = viewStates.appBarTitle
    val isTweetInputExpanded: Boolean
        get() = viewStates.isTweetInputExpanded
    val isTweetInputMenuVisible: LiveData<Boolean> = viewStates.isTweetInputMenuVisible
    override val isFabVisible: LiveData<Boolean> = viewStates.isFabVisible
    internal val navigationEvent: Flow<NavigationEvent> = merge(
        viewStates.initContainer,
        viewStates.navigateToUser,
        viewStates.navEventChannel.receiveAsFlow()
    )

    val isDrawerOpened: LiveData<Boolean> = viewStates.isDrawerOpened
    val currentUser: LiveData<TweetUserItem> = viewStates.currentUser
    val isRegisteredUsersListOpened: LiveData<Boolean> = viewStates.isRegisteredUsersOpened
    val switchableRegisteredUsers: LiveData<Set<TweetUserItem>> =
        viewStates.switchableRegisteredUsers

    internal fun initialEvent(savedState: MainActivityViewState?) {
        eventDispatcher.postEvent(TimelineEvent.Setup(savedState))
    }

    override fun onFabMenuSelected(item: MenuItem) {
        Timber.tag("MainViewModel").d("onFabSelected: $item")
        val selected =
            requireNotNull(currentState.selectedItem) { "selectedItem should not be null." }
        eventDispatcher.postSelectedItemShortcutEvent(item, selected)
    }

    fun collapseTweetInput() {
        check(isTweetInputExpanded)
        eventDispatcher.postEvent(TweetInputEvent.Cancel)
    }

    fun onBackPressed(): Boolean {
        val selectedItem = currentState.selectedItem
        val event = when {
            isDrawerOpened.value == true -> MainActivityEvent.DrawerEvent.Closed
            isTweetInputExpanded -> TweetInputEvent.Cancel
            selectedItem != null -> TimelineEvent.TweetItemSelection.Unselected(selectedItem.owner)
            else -> return false
        }
        eventDispatcher.postEvent(event)
        return true
    }

    fun onAccountSwitcherClicked() {
        eventDispatcher.postEvent(MainActivityEvent.DrawerEvent.AccountSwitchClicked)
    }

    fun onCurrentUserIconClicked() {
        currentUser.value?.let {
            eventDispatcher.postEvent(MainActivityEvent.CurrentUserIconClicked(it))
        }
    }

    fun onDrawerOpened() {
        eventDispatcher.postEvent(MainActivityEvent.DrawerEvent.Opened)
    }

    fun onDrawerClosed() {
        eventDispatcher.postEvent(MainActivityEvent.DrawerEvent.Closed)
    }

    fun onDrawerMenuItemClicked(groupId: Int, itemId: Int, title: CharSequence): Boolean {
        val event = when (itemId) {
//            R.id.menu_item_drawer_home -> MainActivityEvent.DrawerEvent.HomeClicked
            R.id.menu_item_drawer_add_account -> MainActivityEvent.DrawerEvent.AddUserClicked
            R.id.menu_item_drawer_lists -> MainActivityEvent.DrawerEvent.CustomTimelineClicked
            else -> {
                if (groupId == R.id.menu_group_drawer_switchable_accounts) {
                    val user = requireNotNull(
                        switchableRegisteredUsers.value?.find { it.account == title }
                    )
                    MainActivityEvent.DrawerEvent.SwitchableAccountClicked(user.id)
                } else {
                    null
                }
            }
        }
        event?.let { eventDispatcher.postEvent(it) }
        return event != null
    }

    val currentState: MainActivityViewState
        get() = viewStates.current
}

val TweetUserItem.account: String
    get() = "@${screenName}"
