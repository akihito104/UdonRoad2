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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.updateBounds
import androidx.core.view.get
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textview.MaterialTextView

class ExpandableBottomContextMenuView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    private val mainContextMenuList: LinearLayout
    private val moreContextMenuList: RecyclerView
    private val toggle: View?
    private val bottomSheetBehavior = BottomSheetBehavior<View>()
    private val mainMenu = BottomMenu()
    private val moreMenu = BottomMenu()

    var itemClickListener: ItemClickListener? = null
    private val callback: OnClickListener = OnClickListener { v ->
        val item = checkNotNull(mainMenu.findItem(v.id) ?: moreMenu.findItem(v.id))
        itemClickListener?.onItemClicked(item)
    }

    init {
        View.inflate(context, R.layout.view_bottom_menu_list, this).also {
            mainContextMenuList = it.findViewById(R.id.bottom_menu_main)
            moreContextMenuList = it.findViewById(R.id.bottom_menu_more)
        }

        context.withStyledAttributes(
            attributeSet, R.styleable.ExpandableBottomContextMenuView, defStyleAttr, 0
        ) {
            val mainMenuId = getResourceId(R.styleable.ExpandableBottomContextMenuView_menu_main, 0)
            if (mainMenuId != 0) {
                MenuInflater(context).inflate(mainMenuId, mainMenu)
            }
            val moreMenuId = getResourceId(R.styleable.ExpandableBottomContextMenuView_menu_more, 0)
            if (moreMenuId != 0) {
                MenuInflater(context).inflate(moreMenuId, moreMenu)
            }
        }

        setupMenuItems()
        toggle = findViewById(R.id.expandable_bottom_main_toggle)
        bottomSheetBehavior.peekHeight = mainContextMenuList.layoutParams.height
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupMenuItems() {
        mainContextMenuList.setupMainMenu(mainMenu, callback)

        if (moreMenu.size() <= 0) return

        mainContextMenuList.addView(
            Space(context),
            LinearLayout.LayoutParams(0, 0, 1f)
        )
        val toggle = mainContextMenuList.addMainMenuItemView(R.id.expandable_bottom_main_toggle) {
            setImageResource(R.drawable.ic_toggle)
            setOnClickListener {
                bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                    BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
                    BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                    else -> bottomSheetBehavior.state
                }
            }
        }
        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {}

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (!slideOffset.isNaN()) {
                        toggle.rotation = -180f * slideOffset
                    }
                }
            }
        )

        moreContextMenuList.setupMoreMenu(moreMenu, callback)
    }

    private fun findMenuItemById(@IdRes menuId: Int): MenuItem? {
        return mainMenu.findItem(menuId) ?: moreMenu.findItem(menuId)
    }

    fun show() {
        animate()
            .setDuration(250)
            .translationY(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    translationY = height.toFloat()
                    visibility = VISIBLE
                }
            })
            .start()
    }

    fun hide() {
        animate().setDuration(250)
            .translationY(height.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    visibility = INVISIBLE
                }
            })
            .start()
    }

    fun updateMenu(block: UpdateScope.() -> Unit) {
        val updateScope = UpdateScope(this)
        updateScope.block()
        moreContextMenuList.adapter?.let {
            it.notifyDataSetChanged()
            toggle?.isInvisible = it.itemCount == 0
        }
        invalidate()
    }

    class UpdateScope(private val view: ExpandableBottomContextMenuView) {
        fun updateItemOf(@IdRes menuId: Int, block: MenuItem.() -> Unit) {
            val item = view.findMenuItemById(menuId) as? BottomMenu.Item ?: return
            item.block()
            val button = view.mainContextMenuList.findViewById<ImageButton>(item.itemId) ?: return
            button.apply {
                setImageState(item.parseToState(), false)
                isVisible = item.isVisible
            }
        }

        fun changeGroupEnabled(@IdRes groupId: Int, isEnabled: Boolean) {
            view.mainMenu.setGroupEnabled(groupId, isEnabled)
        }
    }

    interface ItemClickListener {
        fun onItemClicked(item: MenuItem)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = bottomSheetBehavior
    }

    companion object {
        private fun LinearLayout.setupMainMenu(mainMenu: BottomMenu, callback: OnClickListener) {
            for (i in 0 until mainMenu.size()) {
                val item = mainMenu[i] as BottomMenu.Item
                addMainMenuItemView(item.itemId) {
                    setIcon(item)
                    setContentDescription(item)
                    setImageState(item.parseToState(), false)
                    setOnClickListener(callback)
                }
            }
        }

        private fun LinearLayout.addMainMenuItemView(
            @IdRes viewId: Int,
            block: AppCompatImageButton.() -> Unit,
        ): AppCompatImageButton {
            val iconSize = resources.getDimensionPixelSize(R.dimen.menu_main_item_size)
            val iconPadding = resources.getDimensionPixelSize(R.dimen.menu_main_item_padding)
            val iconBackground = ContextCompat.getColor(context, android.R.color.transparent)

            val button = AppCompatImageButton(context).apply {
                id = viewId
                setPadding(iconPadding)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(iconBackground)
                block()
            }
            addView(
                button,
                LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            )
            return button
        }

        private fun RecyclerView.setupMoreMenu(moreMenu: BottomMenu, callback: OnClickListener) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = MoreItemAdapter(moreMenu, callback)
        }

        private fun AppCompatImageButton.setIcon(item: BottomMenu.Item) {
            when {
                item.iconRes != 0 -> setImageResource(item.iconRes)
                else -> setImageDrawable(item.icon)
            }
        }

        private fun AppCompatImageButton.setContentDescription(item: BottomMenu.Item) {
            contentDescription = when {
                item.titleRes != 0 -> resources.getString(item.titleRes)
                else -> item.title
            }
        }

        private fun BottomMenu.Item.parseToState(): IntArray {
            return listOfNotNull(
                if (isCheckable) android.R.attr.state_checkable else null,
                if (isChecked) android.R.attr.state_checked else null,
                if (isEnabled) android.R.attr.state_enabled else null,
            ).toIntArray()
        }
    }
}

internal class BottomMenu : Menu {
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
    override fun findItem(id: Int): MenuItem? = items.firstOrNull { it.itemId == id }
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
        outSpecificItems: Array<out MenuItem>?,
    ): Int = unsupported()

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
        private var title: CharSequence? = null,
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
        override fun getIcon(): Drawable? = icon
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
            menuItemClickListener: MenuItem.OnMenuItemClickListener?,
        ): MenuItem = TODO("Not yet implemented")

        override fun getMenuInfo(): ContextMenu.ContextMenuInfo = TODO("Not yet implemented")

        override fun setIntent(intent: Intent?): MenuItem = unsupported()
        override fun getIntent(): Intent = unsupported()
        override fun setShowAsAction(actionEnum: Int): Unit = unsupported()
        override fun setShowAsActionFlags(actionEnum: Int): MenuItem = unsupported()
        override fun setActionView(view: View?): MenuItem = unsupported()
        override fun setActionView(resId: Int): MenuItem = unsupported()
        override fun getActionView(): View = unsupported()
        override fun setActionProvider(actionProvider: ActionProvider?): MenuItem = unsupported()
        override fun getActionProvider(): ActionProvider = unsupported()
        override fun expandActionView(): Boolean = unsupported()
        override fun collapseActionView(): Boolean = unsupported()
        override fun isActionViewExpanded(): Boolean = unsupported()
        override fun setOnActionExpandListener(
            listener: MenuItem.OnActionExpandListener?,
        ): MenuItem = unsupported()
    }

    companion object {
        private fun unsupported(): Nothing = throw UnsupportedOperationException()
    }
}

internal class MoreItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val text: TextView = itemView as TextView
}

private val BottomMenu.visibleItems: List<MenuItem>
    get() = (0 until size()).map { this[it] }.filter { it.isVisible }

internal class MoreItemAdapter(
    private val moreMenu: BottomMenu,
    private val callback: OnClickListener,
) : RecyclerView.Adapter<MoreItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoreItemViewHolder {
        val view = MaterialTextView(parent.context).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.menu_more_list_height)
            )
            updatePadding(
                left = resources.getDimensionPixelSize(R.dimen.menu_more_compound_padding_left)
            )
            compoundDrawablePadding =
                resources.getDimensionPixelSize(R.dimen.menu_more_compound_padding)
            gravity = Gravity.CENTER_VERTICAL
        }
        return MoreItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoreItemViewHolder, position: Int) {
        val context = holder.itemView.context
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.menu_more_icon_size)
        val item = moreMenu.visibleItems[position] as BottomMenu.Item
        holder.itemView.id = item.itemId
        holder.text.text = item.title
        val drawable = when {
            item.icon != null -> item.icon
            item.iconRes != 0 -> {
                AppCompatResources.getDrawable(context, item.iconRes).also {
                    item.icon = it
                }
            }
            else -> null
        }
        drawable?.updateBounds(right = iconSize, bottom = iconSize)
        holder.text.setCompoundDrawables(drawable, null, null, null)
    }

    override fun onViewAttachedToWindow(holder: MoreItemViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.setOnClickListener(callback)
    }

    override fun onViewDetachedFromWindow(holder: MoreItemViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.setOnClickListener(null)
    }

    override fun getItemCount(): Int = moreMenu.visibleItems.size
}
