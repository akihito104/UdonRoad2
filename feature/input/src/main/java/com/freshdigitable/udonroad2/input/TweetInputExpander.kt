/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.input

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

@BindingAdapter("isExpanded", "onExpandAnimationEnd", requireAll = false)
fun View.expand(isExpanded: Boolean?, onExpandAnimationEnd: (() -> Unit)?) {
    when (isExpanded) {
        true -> setupExpendAnim(onExpandAnimationEnd)
        else -> collapseWithAnim()
    }
}

private fun View.setupExpendAnim(onExpandAnimEnd: (() -> Unit)?) {
    measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    when {
        measuredHeight > 0 -> expandWithAnim(onExpandAnimEnd)
        else -> doOnPreDraw { it.expandWithAnim(onExpandAnimEnd) }
    }
}

private fun View.expandWithAnim(onExpandAnimEnd: (() -> Unit)?) {
    val container = parent as View
    val h = measuredHeight
    ValueAnimator.ofInt(-h, 0).apply {
        duration = 200
        interpolator = FastOutSlowInInterpolator()
        doOnStart {
            this@expandWithAnim.visibility = View.VISIBLE
        }
        addUpdateListener {
            val animValue = it.animatedValue as Int
            this@expandWithAnim.translationY = animValue.toFloat()
            container.updateLayoutParams {
                height = h + animValue
            }
        }
        doOnEnd {
            this@expandWithAnim.translationY = 0f
            container.updateLayoutParams {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            onExpandAnimEnd?.invoke()
        }
    }.start()
}

private fun View.collapseWithAnim() {
    val container = parent as View
    val h = measuredHeight
    ValueAnimator.ofInt(-h).apply {
        duration = 200
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            val animValue = it.animatedValue as Int
            this@collapseWithAnim.translationY = animValue.toFloat()
            container.updateLayoutParams {
                height = h + animValue
            }
        }
        doOnEnd {
            this@collapseWithAnim.visibility = View.GONE
        }
    }.start()
}
