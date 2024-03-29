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
import android.view.MotionEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.theme.overlay.MaterialThemeOverlay

class FlingFAB @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.ffabStyle,
) : FloatingActionButton(
    MaterialThemeOverlay.wrap(context, attrs, defStyleAttr, R.style.Widget_FlingFAB),
    attrs,
    defStyleAttr
) {

    private val presenter = FlingFabPresenter.create(this, attrs, defStyleAttr)

    internal var flingEventListener: OnFlingEventListener? = null

    private var old: MotionEvent? = null

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        val listener = flingEventListener ?: return super.onTouchEvent(motionEvent)

        return when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                old = MotionEvent.obtain(motionEvent)
                listener.onFlingEvent(FlingEvent.START)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                return old?.let {
                    val direction = Direction.getDirection(it, motionEvent)
                    listener.onFlingEvent(FlingEvent.MOVING(direction))
                    return true
                } ?: super.onTouchEvent(motionEvent)
            }
            MotionEvent.ACTION_UP -> {
                return old?.let {
                    val direction = Direction.getDirection(it, motionEvent)
                    val isFling = direction != Direction.UNDEFINED
                    val event = if (isFling) FlingEvent.FLING(direction) else FlingEvent.CANCEL
                    listener.onFlingEvent(event)
                    it.recycle()
                    return true
                } ?: super.onTouchEvent(motionEvent)
            }
            MotionEvent.ACTION_CANCEL -> {
                listener.onFlingEvent(FlingEvent.CANCEL)
                old?.recycle()
                return true
            }
            else -> super.onTouchEvent(motionEvent)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter.onAttached()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        presenter.onDetached()
    }

    fun setMenuListener(menuSelectedListener: OnMenuSelectedListener?) {
        presenter.menuSelectedListener = menuSelectedListener
    }

    fun setMode(mode: Mode) {
        presenter.mode = mode
    }

    fun updateMenu(block: ShortcutMenuUpdateScope.() -> Unit) {
        presenter.updateMenu(block)
    }

    enum class Mode { HIDDEN, FAB, TOOLBAR }
}

interface OnMenuSelectedListener {
    fun onMenuSelected(item: ShortcutMenuItem)
}

internal interface OnFlingEventListener {
    fun onFlingEvent(event: FlingEvent)
}

internal sealed class FlingEvent {
    object START : FlingEvent()
    object CANCEL : FlingEvent()
    data class MOVING(internal val direction: Direction) : FlingEvent()
    data class FLING(internal val direction: Direction) : FlingEvent()
}
