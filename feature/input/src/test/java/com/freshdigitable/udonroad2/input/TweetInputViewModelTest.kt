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

import android.text.Editable
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onCancelClicked_whenInputIsCollapsed_menuItemIsNotChanged(): Unit = with(rule) {
            // exercise
            sut.onCancelClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onWriteClicked_then_isVisibleIsTrue(): Unit = with(rule) {
            // exercise
            sut.onWriteClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.isExpanded.value).isTrue()
        }

        @Test
        fun onCancelClicked_then_isVisibleIsFalse(): Unit = with(rule) {
            // setup
            sut.onWriteClicked()

            // exercise
            sut.onCancelClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
        }

        @Test
        fun onTweetTextChanged_addedText_then_menuItemIsSendEnabled(): Unit = with(rule) {
            // setup
            sut.onWriteClicked()

            // exercise
            sut.onTweetTextChanged(editable("a"))

            // verify
            assertThat(sut.text.value).isEqualTo("a")
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_ENABLED)
            assertThat(sut.isExpanded.value).isTrue()
        }

        @Test
        fun onCancelClicked_textAdded_then_textCleared(): Unit = with(rule) {
            // setup
            sut.onWriteClicked()
            sut.onTweetTextChanged(editable("a"))

            // exercise
            sut.onCancelClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onTweetTextChanged_removedText_then_menuItemIsSendDisabled(): Unit = with(rule) {
            // setup
            sut.onWriteClicked()

            // exercise
            sut.onTweetTextChanged(editable("a"))
            sut.onTweetTextChanged(editable(""))

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.isExpanded.value).isTrue()
        }

        @Test
        fun onSendClicked_whenSendIsSucceeded_then_menuItemIsWriteEnabled(): Unit = with(rule) {
            // setup
            setupPost("a")
            sut.onWriteClicked()
            sut.onTweetTextChanged(editable("a"))

            // exercise
            sut.onSendClicked()

            // verify
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onSendClicked_whenSendIsFailed_then_menuItemIsRetryEnabled(): Unit = with(rule) {
            // setup
            setupPost("a", withError = AppTwitterException(403, 123))
            sut.onWriteClicked()
            sut.onTweetTextChanged(editable("a"))

            // exercise
            sut.onSendClicked()

            // verify
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.RETRY_ENABLED)
            assertThat(sut.text.value).isEqualTo("a")
            assertThat(sut.isExpanded.value).isFalse()
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
            assertThat(sut.isExpanded.value).isTrue()
        }
    }
}

@ExperimentalCoroutinesApi
class TweetInputViewModelRule(
    collapsible: Boolean
) : TestWatcher() {
    private val coroutineTestRule = CoroutineTestRule()
    private val repository = MockVerified.create<TweetInputRepository>()

    val sut: TweetInputViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        TweetInputViewModel(
            eventDispatcher,
            TweetInputViewState(
                collapsible,
                TweetInputActions(eventDispatcher),
                repository.mock,
                AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider),
            ),
        )
    }

    override fun starting(description: Description?) {
        super.starting(description)
        with(sut) {
            listOf(inputTask, isExpanded, menuItem, text).forEach { it.observeForever { } }
        }
    }

    fun setupPost(text: String, withError: Throwable? = null) {
        if (withError == null) {
            repository.coSetupResponseWithVerify({ repository.mock.post(text) }, Unit)
        } else {
            repository.coSetupThrowWithVerify({ repository.mock.post(text) }, withError)
        }
    }

    fun editable(text: String): Editable = mockk<Editable>().apply {
        every { this@apply.toString() } returns text
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(coroutineTestRule)
            .around(InstantTaskExecutorRule())
            .around(repository)
            .apply(super.apply(base, description), description)
    }
}
