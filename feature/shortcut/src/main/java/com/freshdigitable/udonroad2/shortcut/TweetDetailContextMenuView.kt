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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ActionProvider
import android.view.ContextMenu
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

class TweetDetailContextMenuView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attributeSet, defStyleAttr) {

    private val mainContextMenuList: LinearLayout
    private val moreContextMenuList: RecyclerView
    private val bottomSheetBehavior = BottomSheetBehavior<View>()
    private val detailMenu = DetailMenu()

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_detail_menu_list, this).also {
            mainContextMenuList = it.findViewById(R.id.detail_menu_main)
            moreContextMenuList = it.findViewById(R.id.detail_menu_more)
        }

        val a = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.TweetDetailContextMenuView,
            defStyleAttr,
            0
        )
        val mainMenuId = a.getResourceId(R.styleable.TweetDetailContextMenuView_menu_main, 0)
        if (mainMenuId != 0) {
            mainContextMenuList.setupMainMenu(mainMenuId)
        }
        a.recycle()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                TODO("Not yet implemented")
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                TODO("Not yet implemented")
            }
        })
        bottomSheetBehavior.peekHeight = mainContextMenuList.layoutParams.height
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun LinearLayout.setupMainMenu(@MenuRes mainMenuId: Int) {
        MenuInflater(context).inflate(mainMenuId, detailMenu)
        val iconSize = resources.getDimensionPixelSize(R.dimen.menu_main_item_size)
        val iconMargin = resources.getDimensionPixelSize(R.dimen.menu_main_item_horizontal_margin)
        val iconBackground = ContextCompat.getColor(context, android.R.color.transparent)

        for (i in 0 until detailMenu.size()) {
            val item = detailMenu.getItem(i) as DetailMenu.Item
            val button = AppCompatImageButton(context).apply {
                setIcon(item)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setContentDescription(item)
                setBackgroundColor(iconBackground)
            }
            val lp = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                updateMargins(left = iconMargin, right = iconMargin)
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(button, lp)
        }
    }

    private fun AppCompatImageButton.setIcon(item: DetailMenu.Item) {
        when {
            item.iconRes != 0 -> setImageResource(item.iconRes)
            else -> setImageDrawable(item.icon)
        }
    }

    private fun AppCompatImageButton.setContentDescription(item: DetailMenu.Item) {
        contentDescription = when {
            item.titleRes != 0 -> resources.getString(item.titleRes)
            else -> item.title
        }
    }

    override fun setOrientation(orientation: Int) {
        check(orientation == VERTICAL) { "VERTICAL is only acceptable." }
        super.setOrientation(orientation)
    }
}

internal class DetailMenu : Menu {
    private val items: MutableList<Item> = mutableListOf()
    override fun size(): Int = items.size

    override fun add(title: CharSequence?): MenuItem {
        val item = Item(title = title)
        items.add(item)
        return item
    }

    override fun add(titleRes: Int): MenuItem {
        val item = Item(titleRes = titleRes)
        items.add(item)
        return item
    }

    override fun add(groupId: Int, itemId: Int, order: Int, title: CharSequence?): MenuItem {
        val item = Item(groupId = groupId, itemId = itemId, order = order, title = title)
        items.add(item)
        return item
    }

    override fun add(groupId: Int, itemId: Int, order: Int, titleRes: Int): MenuItem {
        val item = Item(groupId, itemId, order, titleRes)
        items.add(item)
        return item
    }

    override fun addSubMenu(title: CharSequence?): SubMenu = TODO("Not yet implemented")
    override fun addSubMenu(titleRes: Int): SubMenu = TODO("Not yet implemented")

    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, title: CharSequence?): SubMenu {
        TODO("Not yet implemented")
    }

    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, titleRes: Int): SubMenu {
        TODO("Not yet implemented")
    }

    override fun hasVisibleItems(): Boolean = items.find { it.isVisible } != null
    override fun findItem(id: Int): MenuItem = items.first { it.itemId == id }
    override fun getItem(index: Int): MenuItem = items[index]
    private fun itemsByGroupId(groupId: Int): List<Item> = items.filter { it.groupId == groupId }

    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) {
        itemsByGroupId(group).forEach { it.isCheckable = checkable }
    }

    override fun setGroupVisible(group: Int, visible: Boolean) {
        itemsByGroupId(group).forEach { it.isVisible = visible }
    }

    override fun setGroupEnabled(group: Int, enabled: Boolean) {
        itemsByGroupId(group).forEach { it.isEnabled = enabled }
    }

    override fun removeItem(id: Int) {
        items.removeAll { it.itemId == id }
    }

    override fun removeGroup(groupId: Int) {
        items.removeAll { it.groupId == groupId }
    }

    override fun clear() {
        items.clear()
    }

    override fun addIntentOptions(
        groupId: Int,
        itemId: Int,
        order: Int,
        caller: ComponentName?,
        specifics: Array<out Intent>?,
        intent: Intent?,
        flags: Int,
        outSpecificItems: Array<out MenuItem>?
    ): Int = TODO("Not yet implemented")

    override fun performShortcut(keyCode: Int, event: KeyEvent?, flags: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun isShortcutKey(keyCode: Int, event: KeyEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun performIdentifierAction(id: Int, flags: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun setQwertyMode(isQwerty: Boolean) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    class Item(
        private val groupId: Int = 0,
        private val itemId: Int = 0,
        private val order: Int = 0,
        @StringRes internal var titleRes: Int = 0,
        private var title: CharSequence? = null
    ) : MenuItem {
        override fun getItemId(): Int = itemId
        override fun getGroupId(): Int = groupId
        override fun getOrder(): Int = order

        override fun setTitle(title: CharSequence?): MenuItem {
            this.title = title
            return this
        }

        override fun setTitle(title: Int): MenuItem {
            this.titleRes = title
            return this
        }

        override fun getTitle(): CharSequence = title ?: ""

        private var titleCondensed: CharSequence? = null
        override fun getTitleCondensed(): CharSequence = titleCondensed ?: ""
        override fun setTitleCondensed(title: CharSequence?): MenuItem {
            this.titleCondensed = title
            return this
        }

        private var icon: Drawable? = null
        override fun getIcon(): Drawable = checkNotNull(icon) { "DetailMenu.Item.icon is null." }
        override fun setIcon(icon: Drawable?): MenuItem {
            this.icon = icon
            return this
        }

        @DrawableRes
        internal var iconRes: Int = 0
        override fun setIcon(iconRes: Int): MenuItem {
            this.iconRes = iconRes
            return this
        }

        private var numericShortcut: Char = Char.MIN_VALUE
        override fun getNumericShortcut(): Char = numericShortcut
        override fun setNumericShortcut(numericChar: Char): MenuItem {
            this.numericShortcut = numericChar
            return this
        }

        private var alphabeticShortcut: Char = Char.MIN_VALUE
        override fun getAlphabeticShortcut(): Char = alphabeticShortcut
        override fun setAlphabeticShortcut(alphaChar: Char): MenuItem {
            this.alphabeticShortcut = alphaChar
            return this
        }

        override fun setShortcut(numericChar: Char, alphaChar: Char): MenuItem {
            this.numericShortcut = numericChar
            this.alphabeticShortcut = alphaChar
            return this
        }

        private var checkable: Boolean = false
        override fun isCheckable(): Boolean = checkable
        override fun setCheckable(checkable: Boolean): MenuItem {
            this.checkable = checkable
            return this
        }

        private var checked: Boolean = false
        override fun isChecked(): Boolean = checked
        override fun setChecked(checked: Boolean): MenuItem {
            this.checked = checked
            return this
        }

        private var visible: Boolean = false
        override fun isVisible(): Boolean = visible
        override fun setVisible(visible: Boolean): MenuItem {
            this.visible = visible
            return this
        }

        private var enabled: Boolean = false
        override fun isEnabled(): Boolean = enabled
        override fun setEnabled(enabled: Boolean): MenuItem {
            this.enabled = enabled
            return this
        }

        override fun hasSubMenu(): Boolean = TODO("Not yet implemented")
        override fun getSubMenu(): SubMenu = TODO("Not yet implemented")
        override fun setOnMenuItemClickListener(
            menuItemClickListener: MenuItem.OnMenuItemClickListener?
        ): MenuItem = TODO("Not yet implemented")

        override fun getMenuInfo(): ContextMenu.ContextMenuInfo = TODO("Not yet implemented")
        override fun setIntent(intent: Intent?): MenuItem = TODO("Not yet implemented")
        override fun getIntent(): Intent = TODO("Not yet implemented")
        override fun setShowAsAction(actionEnum: Int): Unit = TODO("Not yet implemented")
        override fun setShowAsActionFlags(actionEnum: Int): MenuItem = TODO("Not yet implemented")
        override fun setActionView(view: View?): MenuItem = TODO("Not yet implemented")
        override fun setActionView(resId: Int): MenuItem = TODO("Not yet implemented")
        override fun getActionView(): View = TODO("Not yet implemented")
        override fun setActionProvider(
            actionProvider: ActionProvider?
        ): MenuItem = TODO("Not yet implemented")

        override fun getActionProvider(): ActionProvider = TODO("Not yet implemented")
        override fun expandActionView(): Boolean = TODO("Not yet implemented")
        override fun collapseActionView(): Boolean = TODO("Not yet implemented")
        override fun isActionViewExpanded(): Boolean = TODO("Not yet implemented")
        override fun setOnActionExpandListener(
            listener: MenuItem.OnActionExpandListener?
        ): MenuItem = TODO("Not yet implemented")
    }
}
