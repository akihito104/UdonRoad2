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

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ActionProvider
import android.view.ContextMenu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

internal class FfabMenuItem(
        private val context: Context,
        private val itemId: Int,
        private val groupId: Int,
        private val order: Int
) : MenuItem {
    internal var direction: Direction? = null

    override fun getItemId(): Int = itemId
    override fun getGroupId(): Int = groupId
    override fun getOrder(): Int = order

    private var title: CharSequence? = null
    private var titleCondensed: CharSequence? = null

    override fun getTitle(): CharSequence = title ?: ""
    override fun getTitleCondensed(): CharSequence = titleCondensed ?: ""

    override fun setTitle(title: CharSequence?): MenuItem {
        this.title = title
        return this
    }

    override fun setTitle(title: Int): MenuItem {
        setTitle(context.getString(title))
        return this
    }

    override fun setTitleCondensed(title: CharSequence?): MenuItem {
        this.titleCondensed = title
        return this
    }

    private var icon: Drawable? = null
    @DrawableRes
    private var iconRes: Int = 0

    override fun setIcon(icon: Drawable?): MenuItem {
        this.icon = icon
        this.iconRes = 0
        return this
    }

    override fun setIcon(@DrawableRes iconRes: Int): MenuItem {
        this.iconRes = iconRes
        this.icon = null
        return this
    }

    override fun getIcon(): Drawable {
        if (icon == null && iconRes != 0) {
            setIcon(AppCompatResources.getDrawable(context, iconRes))
        }
        return icon ?: ColorDrawable()
    }

    private var enabled: Boolean = false
    private var checkable: Boolean = false
    private var checked: Boolean = false
    private var visible: Boolean = false

    override fun isEnabled(): Boolean = enabled
    override fun isCheckable(): Boolean = checkable
    override fun isChecked(): Boolean = checked
    override fun isVisible(): Boolean = visible

    override fun setEnabled(enabled: Boolean): MenuItem {
        this.enabled = enabled
        return this
    }

    override fun setCheckable(checkable: Boolean): MenuItem {
        this.checkable = checkable
        return this
    }

    override fun setChecked(checked: Boolean): MenuItem {
        this.checked = checked
        return this
    }

    override fun setVisible(visible: Boolean): MenuItem {
        this.visible = visible
        return this
    }

    private var intent: Intent? = null
    override fun getIntent(): Intent = intent ?: Intent()

    override fun setIntent(intent: Intent?): MenuItem {
        this.intent = intent
        return this
    }

    override fun hasSubMenu(): Boolean = false

    override fun getSubMenu(): SubMenu = unsupported()
    override fun expandActionView(): Boolean = unsupported()
    override fun getMenuInfo(): ContextMenu.ContextMenuInfo = unsupported()
    override fun getAlphabeticShortcut(): Char = unsupported()
    override fun getActionView(): View = unsupported()
    override fun setOnActionExpandListener(
            listener: MenuItem.OnActionExpandListener?
    ): MenuItem = unsupported()
    override fun setShowAsAction(actionEnum: Int): Unit = unsupported()
    override fun getNumericShortcut(): Char = unsupported()
    override fun isActionViewExpanded(): Boolean = unsupported()
    override fun collapseActionView(): Boolean = unsupported()
    override fun getActionProvider(): ActionProvider = unsupported()

    override fun setActionProvider(actionProvider: ActionProvider?): MenuItem = this
    override fun setNumericShortcut(numericChar: Char): MenuItem = this
    override fun setActionView(view: View?): MenuItem = this
    override fun setActionView(resId: Int): MenuItem = this
    override fun setAlphabeticShortcut(alphaChar: Char): MenuItem = this
    override fun setShortcut(numericChar: Char, alphaChar: Char): MenuItem = this
    override fun setShowAsActionFlags(actionEnum: Int): MenuItem = this
    override fun setOnMenuItemClickListener(
            menuItemClickListener: MenuItem.OnMenuItemClickListener?
    ): MenuItem = this
}
