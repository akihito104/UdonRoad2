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

package com.freshdigitable.udonroad2.tweet

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.user.User
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.regex.Pattern

private val SOURCE_PATTERN = Pattern.compile("<a href=\".*\".*>(.*)</a>")

@BindingAdapter("bindSource")
fun bindSource(v: TextView, source: String?) {
    source?.let {
        val matcher = SOURCE_PATTERN.matcher(it)
        val via = if (matcher.find()) {
            v.context.getString(R.string.tweet_list_item_via, matcher.group(1))
        } else {
            ""
        }
        v.text = via
    }
}

private val NAME_STYLE = StyleSpan(Typeface.BOLD)

@BindingAdapter("bindNames")
fun bindNames(v: TextView, user: User?) {
    if (user == null) {
        return
    }
    val ssb = SpannableStringBuilder(user.name).apply {
        setSpan(NAME_STYLE, 0, user.name.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        append(" @")
        append(user.screenName)
    }
    v.text = ssb
}

@BindingAdapter("bindCreatedAtRelative")
fun bindCreatedAtRelative(v: TextView, createdAt: Instant?) {
    if (createdAt == null) {
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
            LocalDate.from(createdAt).format(DateTimeFormatter.ofPattern(v.context.getString(R.string.created_date)))
        else ->
            LocalDate.from(createdAt).format(DateTimeFormatter.ofPattern(v.context.getString(R.string.created_year_date)))
    }
}
