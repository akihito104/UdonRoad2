/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

internal class FfabMenu(
    private val context: Context
) : Menu {
    private val items = mutableListOf<FfabMenuItem>()

    override fun add(title: CharSequence?): MenuItem = add(0, 0, 0, title)

    override fun add(titleRes: Int): MenuItem = add(context.getString(titleRes))

    override fun add(
        groupId: Int,
        itemId: Int,
        order: Int,
        titleRes: Int
    ): MenuItem = add(groupId, itemId, order, context.getString(titleRes))

    override fun add(
        groupId: Int,
        itemId: Int,
        order: Int,
        title: CharSequence?
    ): MenuItem = FfabMenuItem(context, itemId, groupId, order).also {
        items.add(it)
    }

    override fun removeItem(id: Int) {
        items.firstOrNull { it.itemId == id }?.let { items.remove(it) }
    }

    override fun getItem(index: Int): MenuItem = items[index]

    override fun hasVisibleItems(): Boolean = items.any { it.isVisible }

    override fun findItem(id: Int): MenuItem = items.first { it.itemId == id }

    override fun size(): Int = items.size

    override fun clear() {
        items.clear()
    }

    override fun close() {
        TODO("not implemented")
    }

    internal fun isVisibleByDirection(
        direction: Direction
    ): Boolean = items.any { it.direction == direction }

    internal fun findItem(direction: Direction) = items.firstOrNull { it.direction == direction }

    override fun performIdentifierAction(id: Int, flags: Int): Boolean = unsupported()
    override fun performShortcut(keyCode: Int, event: KeyEvent?, flags: Int): Boolean =
        unsupported()

    override fun setQwertyMode(isQwerty: Boolean) = unsupported()
    override fun addIntentOptions(
        groupId: Int,
        itemId: Int,
        order: Int,
        caller: ComponentName?,
        specifics: Array<out Intent>?,
        intent: Intent?,
        flags: Int,
        outSpecificItems: Array<out MenuItem>?
    ): Int = unsupported()

    override fun addSubMenu(title: CharSequence?): SubMenu = unsupported()
    override fun addSubMenu(titleRes: Int): SubMenu = unsupported()
    override fun addSubMenu(
        groupId: Int,
        itemId: Int,
        order: Int,
        title: CharSequence?
    ): SubMenu = unsupported()

    override fun addSubMenu(
        groupId: Int,
        itemId: Int,
        order: Int,
        titleRes: Int
    ): SubMenu = unsupported()

    override fun setGroupEnabled(group: Int, enabled: Boolean) = unsupported()
    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) =
        unsupported()

    override fun removeGroup(groupId: Int) = unsupported()
    override fun setGroupVisible(group: Int, visible: Boolean) = unsupported()
    override fun isShortcutKey(keyCode: Int, event: KeyEvent?): Boolean = unsupported()
}

internal fun unsupported(): Nothing = throw UnsupportedOperationException()
