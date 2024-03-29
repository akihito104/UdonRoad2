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

package com.freshdigitable.udonroad2.timeline

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.content.res.use
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.textview.MaterialTextView

class IconAttachedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.iconAttachedTextViewStyle,
    defStyleRes: Int = R.style.Widget_IconAttachedTextView
) : MaterialTextView(context, attrs, defStyleAttr) {
    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.IconAttachedTextView, defStyleAttr, defStyleRes
        ).use { a ->
            a.setupIcon()
        }
    }

    private fun TypedArray.setupIcon() {
        val iconRes = getResourceIdOrThrow(R.styleable.IconAttachedTextView_icon)
        val icon = AppCompatResources.getDrawable(context, iconRes)
            ?: getDrawable(R.styleable.IconAttachedTextView_icon) ?: return

        val mutated = icon.mutate()
        val width = icon.intrinsicWidth * lineHeight / icon.intrinsicHeight
        mutated.setBounds(0, 0, width, lineHeight)

        // only setup context: tintIcon is for override ColorStateList but not null
        val iconColor = getColorStateList(R.styleable.IconAttachedTextView_tintIcon)
        if (iconColor != null) {
            DrawableCompat.setTintList(mutated, iconColor)
        }
        setCompoundDrawables(mutated, null, null, null)
    }
}
