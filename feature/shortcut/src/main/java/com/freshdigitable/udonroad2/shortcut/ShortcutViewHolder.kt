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

package com.freshdigitable.udonroad2.shortcut

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.plus
import com.freshdigitable.fabshortcut.ExpandableBottomContextMenuView
import com.freshdigitable.fabshortcut.FlingFAB
import com.freshdigitable.fabshortcut.OnMenuSelectedListener
import com.google.android.material.animation.AnimationUtils.DECELERATE_INTERPOLATOR
import kotlin.math.abs
import kotlin.math.hypot


class ShortcutViewHolder @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    init {
        visibility = INVISIBLE
    }

    @IdRes
    var fabId: Int = 0

    @IdRes
    var toolbarId: Int = 0

    private val fab: FlingFAB by lazy {
        (parent as ViewGroup).findViewById<FlingFAB>(fabId)?.also {
            it.visibility = when (mode) {
                ShortcutViewModel.State.Mode.FAB -> VISIBLE
                else -> INVISIBLE
            }
        } ?: throw IllegalStateException()
    }
    private val toolbar: ExpandableBottomContextMenuView by lazy {
        (parent as ViewGroup).findViewById<ExpandableBottomContextMenuView>(toolbarId)?.also {
            it.visibility = when (mode) {
                ShortcutViewModel.State.Mode.TOOLBAR -> VISIBLE
                else -> INVISIBLE
            }
        } ?: throw IllegalStateException()
    }

    var mode: ShortcutViewModel.State.Mode = ShortcutViewModel.State.Mode.HIDDEN
        set(value) {
            changeMode(value)
            field = value
        }

    private fun changeMode(nextMode: ShortcutViewModel.State.Mode) {
        if (mode == nextMode) {
            return
        }

        when (mode) {
            ShortcutViewModel.State.Mode.HIDDEN -> {
                if (nextMode == ShortcutViewModel.State.Mode.FAB) fab.show()
                else if (nextMode == ShortcutViewModel.State.Mode.TOOLBAR) toolbar.show()
            }
            ShortcutViewModel.State.Mode.FAB -> {
                if (nextMode == ShortcutViewModel.State.Mode.HIDDEN) fab.hide()
                else if (nextMode == ShortcutViewModel.State.Mode.TOOLBAR)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        fabToToolbarAnim()
                    } else {
                        fab.show()
                        toolbar.hide()
                    }
            }
            ShortcutViewModel.State.Mode.TOOLBAR -> {
                if (nextMode == ShortcutViewModel.State.Mode.HIDDEN) toolbar.hide()
                else if (nextMode == ShortcutViewModel.State.Mode.FAB)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        toolbarToFabAnim()
                    } else {
                        toolbar.show()
                        fab.hide()
                    }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun toolbarToFabAnim() {
        val transPoint = transitionPoint
        fab.apply {
            visibility = INVISIBLE
            centerX = transPoint.x
            centerY = transPoint.y
            scaleX = FAB_SCALE
            scaleY = FAB_SCALE
        }

        val revealAnimator = toolbar.circularReveal(reveal = false).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    toolbar.visibility = GONE
                    showFFAB(fab)
                }

                override fun onAnimationCancel(animation: Animator) {
                    toolbar.visibility = GONE
                    fab.visibility = VISIBLE
                    animation.removeListener(this)
                }
            })
        }
        revealAnimator.start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun fabToToolbarAnim() {
        toolbar.apply {
            translationX = 0f
            translationY = 0f
        }
        val transPoint = transitionPoint

        fab.animate()
            .scaleX(FAB_SCALE)
            .scaleY(FAB_SCALE)
            .x(transPoint.x - fab.width / 2)
            .y(transPoint.y - fab.height / 2)
            .setDuration(FAB_MOVE_DURATION)
            .setInterpolator(ACCELERATE_INTERPOLATOR)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    fab.visibility = GONE
                    showToolbar()
                }

                override fun onAnimationCancel(animation: Animator) {
                    fab.visibility = GONE
                    toolbar.visibility = VISIBLE
                    animation.removeListener(this)
                }
            })
            .start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showToolbar() { // XXX
        val revealAnimator = toolbar.circularReveal().apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    toolbar.visibility = VISIBLE
                }
            })
        }
        revealAnimator.start()
    }

    private fun showFFAB(fab: FlingFAB) {
        val animator: ViewPropertyAnimator = fab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .setInterpolator(DECELERATE_INTERPOLATOR)
            .setDuration(FAB_MOVE_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    fab.visibility = VISIBLE
                }
            })
        animator.start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun ExpandableBottomContextMenuView.circularReveal(
        reveal: Boolean = true,
    ): Animator {
        val transPoint = transitionPointLocal
        return ViewAnimationUtils.createCircularReveal(
            mainContextMenuList,
            transPoint.x.toInt(),
            transPoint.y.toInt(),
            if (reveal) calcMinRevealRadius() else calcMaxRevealRadius(),
            if (reveal) calcMaxRevealRadius() else calcMinRevealRadius()
        ).apply {
            duration = TOOLBAR_MOVE_DURATION
        }
    }

    private val transitionPointLocal: PointF
        get() = PointF(toolbar.mainContextMenuList.centerX, toolbar.mainContextMenuList.centerY)
    private val transitionPoint: PointF
        get() = transitionPointLocal + PointF(toolbar.left.toFloat(), toolbar.top.toFloat())

    private fun calcMinRevealRadius(): Float = fab.scaleX * fab.height / 2

    private fun calcMaxRevealRadius(): Float {
        val center = transitionPointLocal
        val radiusX = center.x.coerceAtLeast(abs(toolbar.mainContextMenuList.width - center.x))
        val radiusY = center.y.coerceAtLeast(abs(toolbar.mainContextMenuList.height - center.y))
        return hypot(radiusX.toDouble(), radiusY.toDouble()).toFloat()
    }

    fun setItemListener(listener: OnMenuSelectedListener?) {
        when (listener) {
            null -> {
                fab.setMenuListener(null)
                toolbar.itemClickListener = null
            }
            else -> {
                fab.setMenuListener(listener)
                toolbar.itemClickListener =
                    object : ExpandableBottomContextMenuView.ItemClickListener {
                        override fun onItemClicked(item: MenuItem) {
                            listener.onMenuSelected(item)
                        }
                    }
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(INVISIBLE)
    }

    companion object {
        private const val FAB_SCALE = 1.2f
        private const val FAB_MOVE_DURATION: Long = 80
        private const val TOOLBAR_MOVE_DURATION: Long = 120
        private val ACCELERATE_INTERPOLATOR = AccelerateInterpolator()

        private var View.centerX: Float
            get() = (right - left) / 2f
            set(value) {
                x = value - centerX
            }
        private var View.centerY: Float
            get() = (bottom - top) / 2f
            set(value) {
                y = value - centerY
            }
    }
}
