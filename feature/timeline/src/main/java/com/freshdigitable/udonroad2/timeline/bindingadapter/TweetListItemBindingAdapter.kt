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

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.StyleSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.timeline.MediaThumbnailContainer
import com.freshdigitable.udonroad2.timeline.R
import com.freshdigitable.udonroad2.timeline.mediaViews
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
fun bindNames(v: TextView, user: TweetingUser?) {
    v.text = user?.let {
        SpannableStringBuilder(it.name).apply {
            setSpan(NAME_STYLE, 0, it.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(" @")
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

@BindingAdapter("bindUserIcon")
fun bindUserIcon(v: ImageView, url: String?) {
    if (url == null) {
        Glide.with(v).clear(v)
        return
    }
    Glide.with(v)
        .load(url)
        .into(v)
}

private val movieType: Set<MediaType> = EnumSet.of(MediaType.VIDEO, MediaType.ANIMATED_GIF)

@BindingAdapter("bindMedia")
fun MediaThumbnailContainer.bindMedia(items: List<MediaItem>?) {
    if (items.isNullOrEmpty()) {
        return
    }
    mediaCount = items.size
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
