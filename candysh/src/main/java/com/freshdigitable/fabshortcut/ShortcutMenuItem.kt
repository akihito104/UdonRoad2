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

import android.graphics.drawable.Drawable

interface ShortcutMenuItem : Comparable<ShortcutMenuItem> {
    val itemId: Int
    val groupId: Int
    val order: Int
    val title: CharSequence
    val titleCondensed: CharSequence
    val icon: Drawable
    val direction: Direction?

    var isEnabled: Boolean
    var isChecked: Boolean
    var isCheckable: Boolean
    var isVisible: Boolean

    override fun compareTo(other: ShortcutMenuItem): Int = this.order - other.order
}

internal class ShortcutMenuItemImpl(
    internal val menuItem: FfabMenuItem,
) : ShortcutMenuItem {
    override val itemId: Int
        get() = menuItem.itemId
    override val groupId: Int
        get() = menuItem.groupId
    override val order: Int
        get() = menuItem.order
    override val title: CharSequence
        get() = menuItem.title
    override val titleCondensed: CharSequence
        get() = menuItem.titleCondensed
    override val icon: Drawable
        get() = menuItem.icon
    override val direction: Direction?
        get() = menuItem.direction
    override var isEnabled: Boolean
        get() = menuItem.isEnabled
        set(value) {
            menuItem.isEnabled = value
        }
    override var isChecked: Boolean
        get() = menuItem.isChecked
        set(value) {
            menuItem.isChecked = value
        }
    override var isCheckable: Boolean
        get() = menuItem.isCheckable
        set(value) {
            menuItem.isCheckable = value
        }
    override var isVisible: Boolean
        get() = menuItem.isVisible
        set(value) {
            menuItem.isVisible = value
        }
}
