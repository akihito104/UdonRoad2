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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat


class FlingActionIndicator @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val views: Map<Direction, ImageView> by lazy {
        val v = View.inflate(context, R.layout.view_fling_action_indicator, this)
        mapOf<Direction, ImageView>(
                Direction.UP to v.findViewById(R.id.actionIndicator_up),
                Direction.DOWN to v.findViewById(R.id.actionIndicator_down),
                Direction.LEFT to v.findViewById(R.id.actionIndicator_left),
                Direction.RIGHT to v.findViewById(R.id.actionIndicator_right)
        )
    }

    private val drawables: MutableMap<Direction, Drawable> = mutableMapOf()

    init {
        visibility = View.INVISIBLE
    }

    internal fun onActionSelected(direction: Direction) {
        if (direction === Direction.UNDEFINED) {
            return
        }
        views.values.forEach { it.visibility = View.INVISIBLE }

        if (direction.isOnAxis) {
            translationTo(direction, TransCoefs.ORIGIN)
            return
        }
        drawables[direction]?.let {
            val neighbor = direction.bothNeighbor[0]
            views[neighbor]?.setImageDrawable(it)
            translationTo(neighbor, TransCoefs.ORIGIN)
            return
        }
        when(direction) {
            Direction.UP_RIGHT -> {
                translationTo(Direction.UP, TransCoefs.SECOND_QUAD)
                translationTo(Direction.RIGHT, TransCoefs.FORTH_QUAD)
            }
            Direction.DOWN_RIGHT -> {
                translationTo(Direction.RIGHT, TransCoefs.FIRST_QUAD)
                translationTo(Direction.DOWN, TransCoefs.THIRD_QUAD)
            }
            Direction.DOWN_LEFT -> {
                translationTo(Direction.LEFT, TransCoefs.SECOND_QUAD)
                translationTo(Direction.DOWN, TransCoefs.FORTH_QUAD)
            }
            Direction.UP_LEFT -> {
                translationTo(Direction.UP, TransCoefs.FIRST_QUAD)
                translationTo(Direction.LEFT, TransCoefs.THIRD_QUAD)
            }
            else -> return
        }
    }

    private fun translationTo(direction: Direction, coefs: TransCoefs) {
        translationTo(views[direction], coefs)
    }

    private fun translationTo(icon: View?, coefs: TransCoefs) {
        val ic = icon ?: return

        val dX = paddingLeft + coefs.xCoef * calcContentWidth() - calcCenterX(ic)
        val dY = paddingTop + coefs.yCoef * calcContentHeight() - calcCenterY(ic)
        setTranslation(ic, dX, dY)
        setScale(ic, coefs.scale)
        ic.visibility = View.VISIBLE
    }

    internal fun onActionLeave(direction: Direction) {
        if (direction === Direction.UNDEFINED) {
            return
        }
        if (direction.isOnAxis) {
            resetViewTransforms(direction)
        } else {
            direction.bothNeighbor.forEach { resetViewTransforms(it) }
        }
        views.entries.forEach { (direction, v) ->
            v.setImageDrawable(drawables[direction])
            v.visibility = View.VISIBLE
        }
    }

    private fun resetViewTransforms(direction: Direction) {
        views[direction]?.let { icon ->
            setScale(icon, 1f)
            setTranslation(icon, 0f, 0f)
        }
    }

    private fun calcCenterY(ic: View): Float = ic.y + ic.height / 2

    private fun calcCenterX(ic: View): Float = ic.x + ic.width / 2

    private fun calcContentWidth(): Float = (width - paddingRight - paddingLeft).toFloat()

    private fun calcContentHeight(): Float = (height - paddingTop - paddingBottom).toFloat()

    private fun setScale(icon: View, scale: Float) {
        icon.scaleX = scale
        icon.scaleY = scale
    }

    private fun setTranslation(ic: View, dX: Float, dY: Float) {
        ic.translationX = dX
        ic.translationY = dY
    }

    private var indicatorIconTint: Int = 0

    fun setIndicatorIconTint(indicatorIconTint: Int) {
        this.indicatorIconTint = indicatorIconTint
        drawables.values.forEach { tintIcon(it, this.indicatorIconTint) }
    }

    private fun tintIcon(drawable: Drawable?, @ColorInt color: Int) {
        drawable?.let { DrawableCompat.setTint(it.mutate(), color) }
    }

    private enum class TransCoefs constructor(
            internal val xCoef: Float,
            internal val yCoef: Float,
            internal val scale: Float
    ) {
        ORIGIN(0.5f, 0.5f, 2f),
        FIRST_QUAD(0.75f, 0.25f, 1.5f),
        SECOND_QUAD(0.25f, 0.25f, 1.5f),
        THIRD_QUAD(0.25f, 0.75f, 1.5f),
        FORTH_QUAD(0.75f, 0.75f, 1.5f)
    }
}
