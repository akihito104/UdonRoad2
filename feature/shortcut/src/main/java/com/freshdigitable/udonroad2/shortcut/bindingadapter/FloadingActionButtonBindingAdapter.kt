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

package com.freshdigitable.udonroad2.shortcut.bindingadapter

import androidx.databinding.BindingAdapter
import com.freshdigitable.fabshortcut.ExpandableBottomContextMenuView
import com.freshdigitable.udonroad2.shortcut.MenuItemState
import com.freshdigitable.udonroad2.shortcut.R
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

@BindingAdapter("mode")
fun FloatingActionButton.setMode(mode: ShortcutViewModel.State.Mode?) = when (mode) {
    ShortcutViewModel.State.Mode.FAB -> this.show()
    else -> this.hide()
}

@BindingAdapter("mode")
fun ExpandableBottomContextMenuView.setMode(mode: ShortcutViewModel.State.Mode?) = when (mode) {
    ShortcutViewModel.State.Mode.TOOLBAR -> this.show()
    else -> this.hide()
}

@BindingAdapter("menuItemState")
fun ExpandableBottomContextMenuView.updateMenuItemState(item: MenuItemState?) {
    updateMenu {
        changeGroupEnabled(R.id.menuGroup_detailMain, item?.isMainGroupEnabled ?: false)
        updateItemOf(R.id.detail_main_rt) {
            isChecked = item?.isRetweetChecked ?: false
        }
        updateItemOf(R.id.detail_main_fav) {
            isChecked = item?.isFavChecked ?: false
        }
        updateItemOf(R.id.detail_more_delete) {
            isVisible = item?.isDeleteVisible ?: false
        }
    }
}
