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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@ExperimentalCoroutinesApi
class TweetInputViewModelTest {
    private val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val rule: RuleChain = RuleChain.outerRule(coroutineTestRule)
        .around(InstantTaskExecutorRule())

    @Test
    fun initialValue_collapsableIsTrue_then_menuItemIsWriteEnabled() {
        // setup
        val repository = mockk<TweetInputRepository>().apply {
            every { text } returns MutableStateFlow("")
        }

        // exercise
        val sut = TweetInputViewModel(
            true,
            EventDispatcher(),
            repository,
            AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
        )
        listOf(sut.text, sut.menuItem).forEach { it.observeForever {} }

        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.text.value).isEmpty()
        assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
    }

    @Test
    fun initialValue_collapsableIsFalse_then_menuItemIsSendDisabled() {
        // setup
        val repository = mockk<TweetInputRepository>().apply {
            every { text } returns MutableStateFlow("")
        }

        // exercise
        val sut = TweetInputViewModel(
            false,
            EventDispatcher(),
            repository,
            AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
        )
        listOf(sut.text, sut.menuItem).forEach { it.observeForever {} }

        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.text.value).isEmpty()
        assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
    }
}
