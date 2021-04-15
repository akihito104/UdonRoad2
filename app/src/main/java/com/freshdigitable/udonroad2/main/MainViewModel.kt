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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.input.TweetInputEvent
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.shortcut.ShortcutEventListener
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

internal class MainViewModel(
    private val eventDispatcher: EventDispatcher,
    viewStates: MainViewModelSource,
    private val drawerViewStates: DrawerViewModelSource,
) : ShortcutViewModel,
    ShortcutEventListener by ShortcutEventListener.create(eventDispatcher),
    DrawerViewModel,
    DrawerEventListener by drawerViewStates,
    ViewModel() {

    val mainState: LiveData<MainActivityViewState> =
        viewStates.states.asLiveData(viewModelScope.coroutineContext)
    internal val appBarTitle: LiveData<AppBarTitle> =
        mainState.map { it.appBarTitle }.distinctUntilChanged()
    internal val isTweetInputMenuVisible: LiveData<Boolean> =
        mainState.map { it.isTweetInputMenuVisible }.distinctUntilChanged()

    @Suppress("UNCHECKED_CAST")
    override val shortcutState: LiveData<ShortcutViewModel.State> =
        mainState as LiveData<ShortcutViewModel.State>

    override val drawerState: LiveData<DrawerViewModel.State> =
        drawerViewStates.state.asLiveData(viewModelScope.coroutineContext)

    internal val navigationEvent: Flow<NavigationEvent> = merge(
        viewStates.initContainer,
        drawerViewStates.navEventSource
    )

    internal fun initialEvent() {
        eventDispatcher.postEvent(TimelineEvent.Setup())
    }

    fun collapseTweetInput() {
        check(currentState.isTweetInputExpanded)
        eventDispatcher.postEvent(TweetInputEvent.Cancel)
    }

    fun onBackPressed(): Boolean {
        val selectedItem = currentState.selectedItem
        val event = when {
            drawerState.value?.isOpened == true -> {
                drawerViewStates.hideDrawerMenu.dispatch()
                return true
            }
            currentState.isTweetInputExpanded -> TweetInputEvent.Cancel
            selectedItem != null -> TimelineEvent.TweetItemSelection.Unselected(selectedItem.owner)
            else -> return false
        }
        eventDispatcher.postEvent(event)
        return true
    }

    internal val currentState: MainActivityViewState
        get() = mainState.value ?: throw IllegalStateException()

    val requireSelectedTweetId: TweetId
        get() = requireNotNull(
            currentState.selectedItem?.quoteId ?: currentState.selectedItem?.originalId
        )
}

val TweetUserItem.account: String
    get() = "@$screenName"
