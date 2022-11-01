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
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.updateBounds
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

    internal val mainContextMenuList: LinearLayout
    private val moreContextMenuList: RecyclerView
    private val toggle: View?
    private val bottomSheetBehavior = BottomSheetBehavior<View>()
    private val mainMenu: ShortcutMenu
    private val moreMenu: ShortcutMenu

    internal var itemClickListener: OnMenuSelectedListener? = null
    private val callback: OnClickListener = OnClickListener { v ->
        val item = checkNotNull(findMenuItemById(v.id))
        itemClickListener?.onMenuSelected(item)
    }

    init {
        View.inflate(context, R.layout.view_bottom_menu_list, this).also {
            mainContextMenuList = it.findViewById(R.id.bottom_menu_main)
            moreContextMenuList = it.findViewById(R.id.bottom_menu_more)
        }

        val a = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.ExpandableBottomContextMenuView,
            defStyleAttr,
            0
        )
        try {
            val mainMenuId =
                a.getResourceId(R.styleable.ExpandableBottomContextMenuView_bottomMenu_main, 0)
            mainMenu = ShortcutMenu.inflate(context, mainMenuId)
            val moreMenuId =
                a.getResourceId(R.styleable.ExpandableBottomContextMenuView_bottomMenu_more, 0)
            moreMenu = ShortcutMenu.inflate(context, moreMenuId)
        } finally {
            a.recycle()
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

    private fun findMenuItemById(@IdRes menuId: Int): ShortcutMenuItem? {
        return mainMenu.findByItemId(menuId) ?: moreMenu.findByItemId(menuId)
    }

    internal fun show() {
        animate()
            .setDuration(250)
            .translationY(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    setupToShowAnimForMoreMenu()
                    translationY = mainContextMenuList.height.toFloat()
                    visibility = VISIBLE
                }
            })
            .start()
    }

    internal fun setupToShowAnimForMoreMenu() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    internal fun hide() {
        animate().setDuration(250)
            .translationY(height.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = INVISIBLE
                }
            })
            .start()
    }

    internal fun updateMenu(block: ShortcutMenuUpdateScope.() -> Unit) {
        val updateScope = UpdateScopeImpl(this)
        updateScope.block()
        moreContextMenuList.adapter?.let {
            it.notifyDataSetChanged()
            toggle?.isInvisible = it.itemCount == 0
        }
        invalidate()
    }

    private class UpdateScopeImpl(private val view: ExpandableBottomContextMenuView) :
        ShortcutMenuUpdateScope {
        override fun updateItemOf(@IdRes menuId: Int, block: ShortcutMenuItem.() -> Unit) {
            val item = view.findMenuItemById(menuId) ?: return
            item.block()
            val button = view.mainContextMenuList.findViewById<ImageButton>(item.itemId) ?: return
            button.apply {
                setImageState(item.parseToState(), false)
                isVisible = item.isVisible
            }
        }

        override fun changeGroupEnabled(@IdRes groupId: Int, isEnabled: Boolean) {
            view.mainMenu.setGroupEnabled(groupId, isEnabled)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = bottomSheetBehavior
    }

    internal companion object {
        private fun LinearLayout.setupMainMenu(mainMenu: ShortcutMenu, callback: OnClickListener) {
            for (i in 0 until mainMenu.size()) {
                val item = mainMenu[i]
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

        private fun RecyclerView.setupMoreMenu(moreMenu: ShortcutMenu, callback: OnClickListener) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = MoreItemAdapter(moreMenu, callback)
        }

        private fun AppCompatImageButton.setIcon(item: ShortcutMenuItem) {
            setImageDrawable(item.icon)
        }

        private fun AppCompatImageButton.setContentDescription(item: ShortcutMenuItem) {
            contentDescription = item.title
        }

        private fun ShortcutMenuItem.parseToState(): IntArray {
            return listOfNotNull(
                if (isCheckable) android.R.attr.state_checkable else null,
                if (isChecked) android.R.attr.state_checked else null,
                if (isEnabled) android.R.attr.state_enabled else null,
            ).toIntArray()
        }
    }
}

internal class MoreItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val text: TextView = itemView as TextView
}

internal class MoreItemAdapter(
    private val moreMenu: ShortcutMenu,
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
        val item = moreMenu.visibleItems[position]
        holder.itemView.id = item.itemId
        holder.text.text = item.title
        val drawable = item.icon
        drawable.updateBounds(right = iconSize, bottom = iconSize)
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

interface ShortcutMenuUpdateScope {
    fun updateItemOf(@IdRes menuId: Int, block: ShortcutMenuItem.() -> Unit)
    fun changeGroupEnabled(@IdRes groupId: Int, isEnabled: Boolean)
}
