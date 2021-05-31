/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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
package com.freshdigitable.udonroad2.media

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs
import kotlin.math.sign

class ScalableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private val gestureDetector: GestureDetectorCompat
    private val scaleGestureDetector: ScaleGestureDetector

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        val scaling: Boolean = scaleGestureDetector.isInProgress
        if (scaling) {
            transformMatrix.postScale(scale, scale, focusX, focusY)
        }
        val scrolled: Boolean = gestureDetector.onTouchEvent(event)
        if (scrolled) {
            transformMatrix.postTranslate(transX, transY)
        }
        val invalidated = scaling || scrolled
        if (invalidated) {
            parent.requestDisallowInterceptTouchEvent(true)
            invalidate()
        }
        return invalidated || super.onTouchEvent(event)
    }

    private val imageMat = FloatArray(9)
    override fun onDraw(canvas: Canvas?) {
        if (drawable != null && !transformMatrix.isIdentity) {
            val matrix: Matrix = imageMatrix
            matrix.postConcat(transformMatrix)
            matrix.getValues(imageMat)
            imageMat[Matrix.MSCALE_X] =
                imageMat[Matrix.MSCALE_X].coerceAtLeast(matToFit[Matrix.MSCALE_X])
            imageMat[Matrix.MSCALE_Y] =
                imageMat[Matrix.MSCALE_Y].coerceAtLeast(matToFit[Matrix.MSCALE_Y])
            val maxTransX: Float =
                width - drawable.intrinsicWidth * imageMat[Matrix.MSCALE_X]
            if (abs(maxTransX) < abs(imageMat[Matrix.MTRANS_X])) {
                imageMat[Matrix.MTRANS_X] = maxTransX
            } else if (sign(maxTransX) * imageMat[Matrix.MTRANS_X] < 0) {
                imageMat[Matrix.MTRANS_X] = 0f
            }
            val maxTransY: Float =
                height - drawable.intrinsicHeight * imageMat[Matrix.MSCALE_Y]
            if (abs(maxTransY) < abs(imageMat[Matrix.MTRANS_Y])) {
                imageMat[Matrix.MTRANS_Y] = maxTransY
            } else if (sign(maxTransY) * imageMat[Matrix.MTRANS_Y] < 0) {
                imageMat[Matrix.MTRANS_Y] = 0f
            }
            matrix.setValues(imageMat)
            imageMatrix = matrix
            transformMatrix.reset()
        }
        super.onDraw(canvas)
    }

    private val transformMatrix = Matrix()
    private var scale = 1f
    private var focusX = 0f
    private var focusY = 0f
    private var transX = 0f
    private var transY = 0f
    private val viewRect: RectF = RectF()
    private val drawableRect: RectF = RectF()
    private val matrixToFit = Matrix()
    private val matToFit = FloatArray(9)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateMatrixToFit()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable == null) {
            drawableRect.setEmpty()
        } else {
            drawableRect.set(0f,
                0f,
                drawable.intrinsicWidth.toFloat(),
                drawable.intrinsicHeight.toFloat())
        }
        updateMatrixToFit()
    }

    private fun updateMatrixToFit() {
        if (drawableRect.isEmpty || viewRect.isEmpty) {
            matrixToFit.reset()
            transformMatrix.reset()
        } else {
            matrixToFit.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
            imageMatrix = matrixToFit
            invalidate()
        }
        matrixToFit.getValues(matToFit)
    }

    override fun setImageMatrix(matrix: Matrix) {
        super.setImageMatrix(matrix)
        matrix.getValues(imageMat)
    }

    init {
        scaleGestureDetector =
            ScaleGestureDetector(context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        scale = detector.scaleFactor
                        focusX = detector.focusX
                        focusY = detector.focusY
                        return true
                    }
                })
        gestureDetector =
            GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ): Boolean {
                    if (scale <= 1) {
                        return false
                    }
                    if (e2.eventTime - e1.eventTime < 200 && shouldGoNextPage(distanceX)) {
                        return false
                    }
                    transX = -distanceX
                    transY = -distanceY
                    return true
                }

                private fun shouldGoNextPage(distX: Float): Boolean {
                    if (abs(distX) < 0.1) {
                        return false
                    }
                    if (drawable == null) {
                        return true
                    }
                    if (height.toFloat() == drawable.intrinsicHeight * imageMat[Matrix.MSCALE_Y]) {
                        return true
                    }
                    return if (distX > 0) {
                        val maxTransX: Float =
                            width - drawable.intrinsicWidth * imageMat[Matrix.MSCALE_X]
                        (maxTransX <= 0 && abs(maxTransX - imageMat[Matrix.MTRANS_X]) < 0.01)
                    } else {
                        abs(imageMat[Matrix.MTRANS_X]) < 0.01
                    }
                }
            })
        gestureDetector.setIsLongpressEnabled(false)
        scaleType = ScaleType.MATRIX
    }
}
