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

package com.freshdigitable.udonroad2.shortcut

import android.view.MenuItem
import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher

interface ShortcutViewModel : ShortcutEventListener, ActivityEffectStream {
    val shortcutState: LiveData<State>

    interface State {
        val mode: Mode
        val menuItemState: MenuItemState

        @Deprecated("use `mode`", ReplaceWith("mode"))
        val isVisible: Boolean
            get() = mode != Mode.HIDDEN

        enum class Mode { HIDDEN, FAB, TOOLBAR }
    }
}

data class MenuItemState(
    val isMainGroupEnabled: Boolean = false,
    val isRetweetChecked: Boolean = false,
    val isFavChecked: Boolean = false,
    val isDeleteVisible: Boolean = false,
)

interface ShortcutEventListener {
    fun onShortcutMenuSelected(item: MenuItem, id: TweetId)

    companion object {
        fun create(dispatcher: EventDispatcher): ShortcutEventListener =
            TweetShortcutEventListener(dispatcher)
    }
}

private class TweetShortcutEventListener(
    private val dispatcher: EventDispatcher,
) : ShortcutEventListener {
    override fun onShortcutMenuSelected(item: MenuItem, id: TweetId) {
        dispatcher.postSelectedItemShortcutEvent(item, id)
    }
}
