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
import android.content.Intent
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu

internal class FfabMenu : Menu {
    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeItem(id: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun performIdentifierAction(id: Int, flags: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setGroupEnabled(group: Int, enabled: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItem(index: Int): MenuItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun performShortcut(keyCode: Int, event: KeyEvent?, flags: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeGroup(groupId: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setGroupVisible(group: Int, visible: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(title: CharSequence?): MenuItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(titleRes: Int): MenuItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(groupId: Int, itemId: Int, order: Int, title: CharSequence?): MenuItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(groupId: Int, itemId: Int, order: Int, titleRes: Int): MenuItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isShortcutKey(keyCode: Int, event: KeyEvent?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setQwertyMode(isQwerty: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasVisibleItems(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addSubMenu(title: CharSequence?): SubMenu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addSubMenu(titleRes: Int): SubMenu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, title: CharSequence?): SubMenu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, titleRes: Int): SubMenu {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addIntentOptions(groupId: Int, itemId: Int, order: Int, caller: ComponentName?, specifics: Array<out Intent>?, intent: Intent?, flags: Int, outSpecificItems: Array<out MenuItem>?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findItem(id: Int): MenuItem {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun size(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    internal fun isVisibleByDirection(direction: Direction): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    internal fun dispatchSelectedMenuItem(direction: Direction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}