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

internal class FlingFabPresenter(
    private val fab: FlingFAB,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) {
    private val indicator = FlingActionIndicator(fab.context, attrs, defStyleAttr)
    private val menu = FfabMenu(fab.context)
    internal var menuSelectedListener: OnMenuSelectedListener? = null
    private val marginFromFab: Int

    init {
        val a = fab.context.obtainStyledAttributes(
            attrs,
            R.styleable.FlingFAB, defStyleAttr, R.style.Widget_FlingFAB
        )
        if (a.hasValue(R.styleable.FlingFAB_menu)) {
            val menuRes = a.getResourceId(R.styleable.FlingFAB_menu, 0)
            FfabMenuItemInflater.inflate(fab.context, menu, menuRes)

            for (i in 0 until menu.size) {
                val item = menu[i]
                indicator.setDrawable(checkNotNull(item.direction), item.icon)
            }
        }
        marginFromFab = a.getDimensionPixelSize(R.styleable.FlingFAB_marginFabToIndicator, 0)
        val indicatorTint = a.getColor(R.styleable.FlingFAB_indicatorTint, 0)
        indicator.setBackgroundColor(indicatorTint)
        val indicatorIconTint = a.getColor(R.styleable.FlingFAB_indicatorIconTint, 0)
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

    internal fun onAttached() {
        fab.post { attachIndicator() }
        fab.flingEventListener = flingListener
    }

    internal fun onDetached() {
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
