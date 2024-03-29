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

import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

internal interface FlingFabPresenter {
    val fab: FlingFAB
    var menuSelectedListener: OnMenuSelectedListener?
    var mode: FlingFAB.Mode
    fun onAttached()
    fun onDetached()
    fun updateMenu(block: ShortcutMenuUpdateScope.() -> Unit) {}

    companion object {
        fun create(fab: FlingFAB, attrs: AttributeSet?, defStyleAttr: Int): FlingFabPresenter {
            val a = fab.context.obtainStyledAttributes(
                attrs, R.styleable.FlingFAB, defStyleAttr, R.style.Widget_FlingFAB
            )
            try {
                val p = FlingFabPresenterImpl(fab, attrs, defStyleAttr)
                return when (a.getBoolean(R.styleable.FlingFAB_ffab_bottomMenuEnabled, false)) {
                    true -> ShortcutViewHolder(p, attrs, defStyleAttr)
                    false -> p
                }
            } finally {
                a.recycle()
            }
        }
    }
}

internal class FlingFabPresenterImpl(
    override val fab: FlingFAB,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FlingFabPresenter {
    private val indicator = FlingActionIndicator(fab.context, attrs, defStyleAttr)
    private val menu: ShortcutMenu
    override var menuSelectedListener: OnMenuSelectedListener? = null
    private val marginFromFab: Int

    init {
        fab.setImageResource(R.drawable.ic_add)
        fab.size = FloatingActionButton.SIZE_NORMAL

        val a = fab.context.obtainStyledAttributes(
            attrs, R.styleable.FlingFAB, defStyleAttr, R.style.Widget_FlingFAB
        )
        val menuRes = a.getResourceId(R.styleable.FlingFAB_ffab_menu, 0)
        menu = ShortcutMenu.inflate(fab.context, menuRes)

        for (i in 0 until menu.size()) {
            val item = menu[i]
            indicator.setDrawable(checkNotNull(item.direction), item.icon)
        }
        marginFromFab = a.getDimensionPixelSize(R.styleable.FlingFAB_ffab_marginFabToIndicator, 0)
        val indicatorTint = a.getColor(R.styleable.FlingFAB_ffab_indicatorTint, 0)
        indicator.setBackgroundColor(indicatorTint)
        val indicatorIconTint = a.getColor(R.styleable.FlingFAB_ffab_indicatorIconTint, 0)
        indicator.setIndicatorIconTint(indicatorIconTint)
        a.recycle()
    }

    private val flingListener = object : OnFlingEventListener {
        private var prevDirection: Direction = Direction.UNDEFINED

        override fun onFlingEvent(event: FlingEvent) {
            when (event) {
                is FlingEvent.START -> {
                    indicator.onActionLeave(prevDirection)
                    prevDirection = Direction.UNDEFINED
                    indicator.visibility = View.VISIBLE
                }
                is FlingEvent.MOVING -> {
                    if (prevDirection == event.direction) {
                        return
                    }
                    indicator.onActionLeave(prevDirection)
                    if (menu.isVisibleByDirection(event.direction)) {
                        indicator.onActionSelected(event.direction)
                    }
                    prevDirection = event.direction
                }
                is FlingEvent.FLING -> {
                    indicator.postDelayed({ indicator.visibility = View.INVISIBLE }, 200)
                    val item = menu.findByDirection(event.direction) ?: return
                    menuSelectedListener?.onMenuSelected(item)
                }
                is FlingEvent.CANCEL -> {
                    indicator.onActionLeave(prevDirection)
                    prevDirection = Direction.UNDEFINED
                    indicator.visibility = View.INVISIBLE
                }
            }
        }
    }

    override var mode: FlingFAB.Mode = FlingFAB.Mode.HIDDEN
        set(value) {
            require(value != FlingFAB.Mode.TOOLBAR) { "Mode.TOOLBAR is not accepted." }

            if (field == value) {
                return
            }
            when (field) {
                FlingFAB.Mode.HIDDEN -> {
                    if (value == FlingFAB.Mode.FAB) fab.show()
                }
                FlingFAB.Mode.FAB -> {
                    if (value == FlingFAB.Mode.HIDDEN) fab.hide()
                }
                FlingFAB.Mode.TOOLBAR -> throw IllegalStateException()
            }
            field = value
        }

    override fun onAttached() {
        fab.post {
            attachIndicator()
            fab.visibility = mode.visibilityForFab
        }
        fab.flingEventListener = flingListener
    }

    override fun onDetached() {
        fab.flingEventListener = null
    }

    private fun attachIndicator() {
        if (indicator.parent != null) {
            return
        }
        val indicatorLp = indicator.layoutParams
        val lp = when (val fabLp = fab.layoutParams) {
            is ConstraintLayout.LayoutParams -> ConstraintLayout.LayoutParams(indicatorLp).apply {
                bottomToTop = fab.id
                startToStart = fab.id
                endToEnd = fab.id
                bottomMargin = marginFromFab
            }
            is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(indicatorLp).apply {
                gravity = fabLp.gravity
                setMarginFromEdge()
            }
            is CoordinatorLayout.LayoutParams -> CoordinatorLayout.LayoutParams(indicatorLp).apply {
                gravity = fabLp.gravity
                setMarginFromEdge()
            }
            else -> unsupported()
        }
        (fab.parent as ViewGroup).addView(indicator, lp)
    }

    private fun ViewGroup.MarginLayoutParams.setMarginFromEdge() {
        val fabLp = fab.layoutParams as ViewGroup.MarginLayoutParams
        bottomMargin = marginFromFab + fab.height + fabLp.bottomMargin
    }
}

internal val FlingFAB.Mode.visibilityForFab: Int
    get() = when (this) {
        FlingFAB.Mode.FAB -> View.VISIBLE
        else -> View.INVISIBLE
    }

internal val FlingFAB.Mode.visibilityForToolbar: Int
    get() = when (this) {
        FlingFAB.Mode.TOOLBAR -> View.VISIBLE
        else -> View.INVISIBLE
    }
