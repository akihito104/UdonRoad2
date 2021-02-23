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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.input.TweetInputEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.shortcut.postSelectedItemShortcutEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import timber.log.Timber

internal class MainViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: MainActivityViewStates,
    private val drawerViewStates: DrawerViewModelSource,
) : ViewModel(), ShortcutViewModel, DrawerActionListener by drawerViewStates {

    val mainViewModelState = viewStates.states.asLiveData(viewModelScope.coroutineContext)
    val navIconType: LiveData<NavigationIconType> = mainViewModelState.map { it.navIconType }
    val appBarTitle: LiveData<AppBarTitle> = mainViewModelState.map { it.appBarTitle }
    val isTweetInputExpanded: Boolean
        get() = mainViewModelState.value?.isTweetInputExpanded ?: false
    val isTweetInputMenuVisible: LiveData<Boolean> =
        mainViewModelState.map { it.isTweetInputMenuVisible }
    override val isFabVisible: LiveData<Boolean> = mainViewModelState.map { it.isShortcutVisible }
    internal val navigationEvent: Flow<NavigationEvent> = merge(
        viewStates.initContainer,
        viewStates.navigateToUser,
        drawerViewStates.navEventSource
    )

    val drawerState: LiveData<DrawerViewState> =
        drawerViewStates.state.asLiveData(viewModelScope.coroutineContext)

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

    override fun onBackPressed(): Boolean {
        val selectedItem = currentState.selectedItem
        val event = when {
            drawerState.value?.isOpened == true -> {
                return drawerViewStates.onBackPressed()
            }
            isTweetInputExpanded -> TweetInputEvent.Cancel
            selectedItem != null -> TimelineEvent.TweetItemSelection.Unselected(selectedItem.owner)
            else -> return false
        }
        eventDispatcher.postEvent(event)
        return true
    }

    fun onCurrentUserIconClicked() {
        drawerState.value?.currentUser?.let {
            eventDispatcher.postEvent(MainActivityEvent.CurrentUserIconClicked(it))
        }
    }

    val currentState: MainActivityViewState
        get() = mainViewModelState.value ?: throw IllegalStateException()
}

val TweetUserItem.account: String
    get() = "@${screenName}"
