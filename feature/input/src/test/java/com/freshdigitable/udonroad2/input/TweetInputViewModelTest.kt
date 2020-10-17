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
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@ExperimentalCoroutinesApi
@RunWith(Enclosed::class)
class TweetInputViewModelTest {

    class WhenCollapsibleIsTrue {
        @get:Rule
        val rule = TweetInputViewModelRule(collapsible = true)

        @Test
        fun initialValue(): Unit = with(rule) {
            // verify
            assertThat(sut).isNotNull()
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isVisible.value).isFalse()
        }

        @Test
        fun onWriteClicked_then_isVisibleIsTrue(): Unit = with(rule) {
            // exercise
            sut.onWriteClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.isVisible.value).isTrue()
        }
    }

    class WhenCollapsibleIsFalse {
        @get:Rule
        val rule = TweetInputViewModelRule(collapsible = false)

        @Test
        fun initialValue(): Unit = with(rule) {
            // verify
            assertThat(sut).isNotNull()
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.isVisible.value).isTrue()
        }
    }
}

@ExperimentalCoroutinesApi
class TweetInputViewModelRule(
    collapsible: Boolean
) : TestWatcher() {
    private val coroutineTestRule = CoroutineTestRule()
    private val repository = MockVerified.create<TweetInputRepository>().apply {
        setupResponseWithVerify({ mock.text }, MutableStateFlow(""))
    }

    val sut: TweetInputViewModel by lazy {
        TweetInputViewModel(
            collapsible,
            EventDispatcher(),
            repository.mock,
            AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
        )
    }

    override fun starting(description: Description?) {
        super.starting(description)
        with(sut) {
            listOf(isVisible, menuItem, text).forEach { it.observeForever { } }
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(coroutineTestRule)
            .around(InstantTaskExecutorRule())
            .around(repository)
            .apply(super.apply(base, description), description)
    }
}
