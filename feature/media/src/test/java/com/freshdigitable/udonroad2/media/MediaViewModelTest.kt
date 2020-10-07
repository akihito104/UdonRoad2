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

package com.freshdigitable.udonroad2.media

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MediaViewModelTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun initialValue() {
        val sut = MediaViewModel(
            mockk(),
            ApplicationProvider.getApplicationContext(),
            AppExecutor(dispatcher = coroutineRule.coroutineContextProvider)
        )

        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.tweet.value).isNull()
        assertThat(sut.currentPosition.value).isNull()
        assertThat(sut.systemUiVisibility.value).isNull()
        assertThat(sut.isInImmersive.value).isNull()
        assertThat(sut.titleText.value).isNull()
    }
}
