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

package com.freshdigitable.udonroad2.timeline.bindingadapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import kotlin.math.roundToInt

internal class RefinedImageSpan(
    d: Drawable,
    verticalAlignment: Int,
    private val marginStart: Int,
    private val marginEnd: Int,
) : ImageSpan(d, verticalAlignment) {

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val bounds: Rect = drawable.bounds
        fm?.apply {
            val pfm: Paint.FontMetrics = paint.fontMetrics
            ascent = when (verticalAlignment) {
                ALIGN_BASELINE -> -bounds.bottom
                ALIGN_BOTTOM -> (bounds.bottom * pfm.top / pfm.height).roundToInt()
                ALIGN_CENTER -> (pfm.top - (bounds.bottom - pfm.height) / 2).roundToInt()
                else -> ascent
            }
            descent = (bounds.bottom + ascent).coerceAtLeast(0)
            top = ascent
            bottom = descent
        }
        return marginStart + bounds.right + marginEnd
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        baseline: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val drawableHeight = drawable.bounds.bottom
        canvas.save()
        val transY: Int = when (verticalAlignment) {
            ALIGN_BASELINE -> baseline - drawableHeight
            ALIGN_BOTTOM -> bottom - drawableHeight - paint.fontMetricsInt.leading
            ALIGN_CENTER -> {
                val center: Float =
                    baseline + (paint.fontMetrics.bottom + paint.fontMetrics.top) / 2
                (center - drawableHeight / 2).toInt()
            }
            else -> top
        }
        canvas.translate(x + marginStart, transY.toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }

    companion object {
        const val ALIGN_CENTER = 10

        private val Paint.FontMetrics.height: Float get() = bottom - top
    }
}
