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

package com.freshdigitable.udonroad2.timeline.bindingadapter

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.StyleSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.freshdigitable.udonroad2.media.MediaThumbnailContainer
import com.freshdigitable.udonroad2.media.mediaViews
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.TweetMediaItem
import com.freshdigitable.udonroad2.model.thumbMediaUrl
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.R
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.EnumSet
import java.util.regex.Pattern

private val SOURCE_PATTERN = Pattern.compile("<a href=\".*\".*>(.*)</a>")

@BindingAdapter("bindSource")
fun bindSource(v: TextView, source: String?) {
    v.text = source?.let {
        val matcher = SOURCE_PATTERN.matcher(it)
        if (matcher.find()) {
            v.context.getString(R.string.tweet_list_item_via, matcher.group(1))
        } else {
            ""
        }
    }
}

private val NAME_STYLE = StyleSpan(Typeface.BOLD)

@BindingAdapter("bindNames")
fun bindNames(v: TextView, user: TweetUserItem?) {
    v.text = user?.let {
        SpannableStringBuilder(it.name).apply {
            setSpan(NAME_STYLE, 0, it.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (TextViewCompat.getMaxLines(v) == 2) {
                append("\n")
            } else {
                append(" ")
            }
            append("@")
            append(it.screenName)
        }
    }
}

@BindingAdapter("bindCreatedAtAbsolute")
fun bindCreatedAtAbsolute(v: TextView, createdAt: Instant?) {
    v.text = if (createdAt == null) {
        ""
    } else {
        DateUtils.formatDateTime(
            v.context, createdAt.toEpochMilli(),
            DateUtils.FORMAT_SHOW_YEAR.or(DateUtils.FORMAT_SHOW_DATE).or(DateUtils.FORMAT_SHOW_TIME)
        )
    }
}

@BindingAdapter("bindCreatedAtRelative")
fun bindCreatedAtRelative(v: TextView, createdAt: Instant?) {
    if (createdAt == null) {
        v.text = ""
        return
    }
    val delta = Duration.between(createdAt, Instant.now())
    v.text = when {
        delta.seconds <= 1 -> v.context.getString(R.string.created_now)
        delta.seconds < 60 -> v.context.getString(R.string.created_seconds_ago, delta.seconds)

        delta.toMinutes() < 45 -> delta.toMinutes().toInt().let { min ->
            v.context.resources.getQuantityString(R.plurals.created_minutes_ago, min, min)
        }
        delta.toMinutes() < 105 ->
            v.context.resources.getQuantityString(R.plurals.created_hours_ago, 1, 1)

        delta.toHours() < 24 -> delta.toHours().toInt().let { hours ->
            v.context.resources.getQuantityString(R.plurals.created_hours_ago, hours, hours)
        }
        delta.toDays() < 30 ->
            DateTimeFormatter.ofPattern(v.context.getString(R.string.created_date))
                .format(LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()))
        else ->
            DateTimeFormatter.ofPattern(v.context.getString(R.string.created_year_date))
                .format(LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()))
    }
}

private val roundedCornersTransforms: HashMap<Int, RoundedCorners> = hashMapOf()

private fun Resources.getRoundedCornersTransform(cornerRadius: Int? = null): RoundedCorners {
    val cr = cornerRadius ?: getDimensionPixelSize(R.dimen.icon_corner_radius)
    return roundedCornersTransforms.getOrPut(cr) { RoundedCorners(cr) }
}

@BindingAdapter("bindUserIcon", "corner_radius", requireAll = false)
fun bindUserIcon(v: ImageView, url: String?, cornerRadius: Float?) {
    if (url == null) {
        Glide.with(v).clear(v)
        return
    }
    Glide.with(v)
        .load(url)
        .placeholder(R.drawable.ic_person_outline_black)
        .transform(v.resources.getRoundedCornersTransform(cornerRadius?.toInt()))
        .into(v)
}

private val movieType: Set<MediaType> = EnumSet.of(MediaType.VIDEO, MediaType.ANIMATED_GIF)

@BindingAdapter("bindMedia", "hideForPossiblySensitive", requireAll = false)
fun MediaThumbnailContainer.bindMedia(
    items: List<TweetMediaItem>?,
    hideForPossiblySensitive: Boolean?,
) {
    if (items.isNullOrEmpty()) {
        return
    }
    mediaCount = items.size
    if (hideForPossiblySensitive == true) {
        mediaViews.forEach {
            Glide.with(it)
                .load(R.drawable.ic_whatshot)
                .into(it)
        }
    } else {
        items.zip(mediaViews) { item, view ->
            val option = RequestOptions.centerCropTransform()
                .placeholder(ColorDrawable(Color.LTGRAY)).apply {
                    if (itemWidth > 0) {
                        override(itemWidth, height)
                    }
                }
            view.isMovie = movieType.contains(item.type)
            Glide.with(view)
                .load(item.thumbMediaUrl)
                .apply(option)
                .into(view)
        }
    }
}

@BindingAdapter("rtUserText")
fun TextView.bindRtUserIcon(user: TweetUserItem?) {
    if (user == null) {
        text = ""
        return
    }
    val iconSpan = context.getString(R.string.rt_user_icon)
    val rtText = context.getString(R.string.format_rt_user_with_icon, iconSpan, user.screenName)
    val start = rtText.indexOf(iconSpan)
    val iconSize = resources.getDimensionPixelSize(R.dimen.icon_size_small)
    Glide.with(this)
        .load(user.iconUrl)
        .placeholder(R.drawable.ic_person_outline_black)
        .transform(resources.getRoundedCornersTransform())
        .into(object : CustomTarget<Drawable>() {
            private fun createRtSpannedText(resource: Drawable): SpannableStringBuilder {
                resource.setBounds(0, 0, iconSize, iconSize)
                val imageSpan = RefinedImageSpan(resource, RefinedImageSpan.ALIGN_CENTER, 0, 0)
                val end = start + iconSpan.length
                return SpannableStringBuilder(rtText).apply {
                    setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            override fun onLoadStarted(placeholder: Drawable?) {
                placeholder?.let { this@bindRtUserIcon.text = createRtSpannedText(it) }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                errorDrawable?.let { this@bindRtUserIcon.text = createRtSpannedText(it) }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                placeholder?.let { this@bindRtUserIcon.text = createRtSpannedText(it) }
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                this@bindRtUserIcon.text = createRtSpannedText(resource)
            }
        })
}
