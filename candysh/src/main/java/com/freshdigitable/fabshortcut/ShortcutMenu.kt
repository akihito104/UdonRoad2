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

package com.freshdigitable.fabshortcut

import android.content.Context
import android.view.Menu
import androidx.annotation.MenuRes

internal interface ShortcutMenu : Menu {
    fun findByDirection(direction: Direction): ShortcutMenuItem?
    fun isVisibleByDirection(direction: Direction): Boolean = findByDirection(direction) != null
    fun findByItemId(id: Int): ShortcutMenuItem?

    operator fun get(index: Int): ShortcutMenuItem

    companion object {
        fun inflate(context: Context, @MenuRes menuRes: Int): ShortcutMenu {
            val ffabMenu = FfabMenu(context)
            if (menuRes != 0) {
                FfabMenuItemInflater.inflate(context, ffabMenu, menuRes)
            }
            return ffabMenu
        }
    }
}

internal val ShortcutMenu.visibleItems: List<ShortcutMenuItem>
    get() = (0 until size()).map { this[it] }.filter { it.isVisible }
