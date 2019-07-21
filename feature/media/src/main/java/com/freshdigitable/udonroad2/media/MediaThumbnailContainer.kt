/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.updateBounds
import androidx.core.graphics.withSave
import androidx.core.view.children

class MediaThumbnailContainer @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attr, defStyleAttr) {

    private val margin: Int

    init {
        orientation = HORIZONTAL
        val a = context.obtainStyledAttributes(
            attr,
            R.styleable.MediaThumbnailContainer,
            defStyleAttr,
            R.style.Widget_MediaThumbnailContainer
        )
        margin = a.getDimension(R.styleable.MediaThumbnailContainer_gapWidth, 0f).toInt()
        a.recycle()
    }

    var mediaCount: Int = 0
        set(value) {
            updateChildren(value)
            field = value
        }
    val itemWidth
        get() = if (mediaCount > 0) (width - margin * (mediaCount - 1)) / mediaCount else 0

    var itemClickListener: MediaItemClickListener? = null

    private fun updateChildren(count: Int) {
        val current = childCount
        val needed = count - current
        if (needed > 0) {
            repeat(needed) {
                val grid = if (current + it == 0) 0 else margin

                val lp = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        marginStart = grid
                    } else {
                        leftMargin = grid
                    }
                }
                addView(MediaThumbnailView(context), lp)
            }
        }
        children.forEachIndexed { i, child ->
            child.visibility = if (i < count) View.VISIBLE else GONE
            child.setOnClickListener(
                if (i < count) OnClickListener { v -> itemClickListener?.onMediaItemClicked(v, i) }
                else null)
        }
    }

    interface MediaItemClickListener {
        fun onMediaItemClicked(v: View, index: Int)
    }
}

class MediaThumbnailView @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attr, defStyleAttr) {

    var isMovie: Boolean = false
    private var movieIcon: Drawable? = null

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        if (isMovie && canvas != null) {
            val movieIcon = this.movieIcon
                ?: AppCompatResources.getDrawable(context, R.drawable.ld_play_icon)?.apply {
                    updateBounds(
                        left = 0,
                        top = 0,
                        right = intrinsicWidth,
                        bottom = intrinsicHeight
                    )
                }
                ?: return
            val dx = (width - movieIcon.intrinsicWidth) / 2f
            val dy = (height - movieIcon.intrinsicHeight) / 2f
            canvas.withSave {
                translate(dx, dy)
                movieIcon.draw(this)
            }
        }
    }
}

val MediaThumbnailContainer.mediaViews: Iterable<MediaThumbnailView>
    get() = object : Iterable<MediaThumbnailView> {
        override fun iterator(): Iterator<MediaThumbnailView> {
            return object : Iterator<MediaThumbnailView> {
                private var index = 0

                override fun hasNext(): Boolean = index < childCount

                override fun next(): MediaThumbnailView {
                    val child = getChildAt(index) as MediaThumbnailView
                    index += 1
                    return child
                }
            }
        }
    }
