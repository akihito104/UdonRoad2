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
    private val context: Context,
) : Menu, ShortcutMenu {
    private val items = mutableListOf<ShortcutMenuItemImpl>()

    override fun add(title: CharSequence?): MenuItem = add(0, 0, 0, title)

    override fun add(titleRes: Int): MenuItem = add(context.getString(titleRes))

    override fun add(
        groupId: Int,
        itemId: Int,
        order: Int,
        titleRes: Int,
    ): MenuItem = add(groupId, itemId, order, context.getString(titleRes))

    override fun add(
        groupId: Int,
        itemId: Int,
        order: Int,
        title: CharSequence?,
    ): MenuItem = FfabMenuItem(context, itemId, groupId, order).also {
        items.add(ShortcutMenuItemImpl(it))
    }

    override fun removeItem(id: Int) {
        items.firstOrNull { it.itemId == id }?.let { items.remove(it) }
    }

    override fun getItem(index: Int): MenuItem = items[index].menuItem

    override fun hasVisibleItems(): Boolean = items.any { it.isVisible }

    override fun findItem(id: Int): MenuItem = items.first { it.itemId == id }.menuItem

    override fun size(): Int = items.size

    override fun clear() {
        items.clear()
    }

    override fun findByItemId(id: Int): ShortcutMenuItem? {
        return items.firstOrNull { it.itemId == id }
    }

    override operator fun get(index: Int): ShortcutMenuItem = items[index]

    override fun findByDirection(direction: Direction): ShortcutMenuItem? =
        items.firstOrNull { it.direction == direction }

    private fun itemsByGroupId(groupId: Int): List<ShortcutMenuItem> =
        items.filter { it.groupId == groupId }

    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) {
        itemsByGroupId(group).forEach { it.isCheckable = checkable }
    }

    override fun setGroupVisible(group: Int, visible: Boolean) {
        itemsByGroupId(group).forEach { it.isVisible = visible }
    }

    override fun setGroupEnabled(group: Int, enabled: Boolean) {
        itemsByGroupId(group).forEach { it.isEnabled = enabled }
    }

    override fun removeGroup(groupId: Int) {
        items.removeAll { it.groupId == groupId }
    }

    override fun performIdentifierAction(id: Int, flags: Int): Boolean = unsupported()
    override fun performShortcut(keyCode: Int, event: KeyEvent?, flags: Int): Boolean =
        unsupported()

    override fun close() = unsupported()

    override fun setQwertyMode(isQwerty: Boolean) = unsupported()
    override fun addIntentOptions(
        groupId: Int,
        itemId: Int,
        order: Int,
        caller: ComponentName?,
        specifics: Array<out Intent>?,
        intent: Intent?,
        flags: Int,
        outSpecificItems: Array<out MenuItem>?,
    ): Int = unsupported()

    override fun addSubMenu(title: CharSequence?): SubMenu = unsupported()
    override fun addSubMenu(titleRes: Int): SubMenu = unsupported()
    override fun addSubMenu(
        groupId: Int,
        itemId: Int,
        order: Int,
        title: CharSequence?,
    ): SubMenu = unsupported()

    override fun addSubMenu(
        groupId: Int,
        itemId: Int,
        order: Int,
        titleRes: Int,
    ): SubMenu = unsupported()

    override fun isShortcutKey(keyCode: Int, event: KeyEvent?): Boolean = unsupported()
}

internal fun unsupported(): Nothing = throw UnsupportedOperationException()
