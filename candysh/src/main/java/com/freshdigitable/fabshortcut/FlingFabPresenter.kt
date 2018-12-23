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
import android.util.AttributeSet
import android.view.View

internal class FlingFabPresenter(
        private val fab: FlingFAB,
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) {
    private val indicator = FlingActionIndicator(context, attrs, defStyleAttr)
    private val menu = FfabMenu(context)

    init {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.FlingFAB, defStyleAttr, R.style.Widget_FlingFAB)
        if (a.hasValue(R.styleable.FlingFAB_menu)) {
            val menuRes = a.getResourceId(R.styleable.FlingFAB_menu, 0)
            FfabMenuItemInflater.inflate(context, menu, menuRes)
        }
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
                    menu.dispatchSelectedMenuItem(event.direction)
                    indicator.postDelayed({
                        indicator.visibility = View.INVISIBLE
                    }, 200)
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
        fab.flingEventListener = flingListener
    }

    internal fun onDetached() {
        fab.flingEventListener = null
    }
}
