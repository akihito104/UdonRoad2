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

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.freshdigitable.udonroad2.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class TweetListItemBindingAdapterTest {

    @Test
    fun studyDateTimeFormatting() {
        val context = InstrumentationRegistry.getTargetContext()
        val createdAt = Instant.parse("2018-10-04T11:45:06Z")

        val formatter = DateTimeFormatter.ofPattern(context.getString(R.string.created_date))
        val actual = formatter.format(LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()))

        assertThat(actual).isEqualTo("4 Oct")
    }

    @Test
    fun studyDateTimeFormatting2() {
        val context = InstrumentationRegistry.getTargetContext()
        val createdAt = Instant.parse("2018-10-04T11:45:06Z")

        val formatter = DateTimeFormatter.ofPattern(context.getString(R.string.created_year_date))
        val actual = formatter.format(LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()))

        assertThat(actual).isEqualTo("4 Oct 18")
    }
}
