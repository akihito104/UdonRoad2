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
import com.freshdigitable.udonroad2.input.TweetInputEvent
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.freshdigitable.udonroad2.shortcut.postSelectedItemShortcutEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import timber.log.Timber

class MainViewModel(
    private val eventDispatcher: EventDispatcher,
    private val viewStates: MainActivityViewStates,
) : ViewModel(), ShortcutViewModel {

    override val isFabVisible: LiveData<Boolean> = viewStates.isFabVisible

    internal fun initialEvent(savedState: MainActivityViewState?) {
        eventDispatcher.postEvent(TimelineEvent.Setup(savedState))
    }

    override fun onFabMenuSelected(item: MenuItem) {
        Timber.tag("MainViewModel").d("onFabSelected: $item")
        val selected =
            requireNotNull(currentState?.selectedItem) { "selectedItem should not be null." }
        eventDispatcher.postSelectedItemShortcutEvent(item, selected)
    }

    fun onTweetSendClicked() {
        eventDispatcher.postEvent(TweetInputEvent.Send)
    }

    fun onBackPressed() {
        val selectedItem = currentState?.selectedItem
        val event = if (selectedItem != null) {
            TimelineEvent.TweetItemSelection.Unselected(selectedItem.owner)
        } else {
            CommonEvent.Back
        }
        eventDispatcher.postEvent(event)
    }

    val currentState: MainActivityViewState?
        get() = viewStates.current

    override fun onCleared() {
        super.onCleared()
        viewStates.clear()
    }
}
