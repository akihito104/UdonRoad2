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
import com.freshdigitable.udonroad2.model.app.AppTwitterException
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

        @Test
        fun onCloseClicked_then_isVisibleIsFalse(): Unit = with(rule) {
            // setup
            setupClear()
            sut.onWriteClicked()

            // exercise
            sut.onCloseClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.isVisible.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
        }

        @Test
        fun onTweetTextChanged_addedText_then_menuItemIsSendEnabled(): Unit = with(rule) {
            // setup
            setupUpdateText("a")
            sut.onWriteClicked()

            // exercise
            sut.onTweetTextChanged("a")

            // verify
            assertThat(sut.text.value).isEqualTo("a")
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_ENABLED)
            assertThat(sut.isVisible.value).isTrue()
        }

        @Test
        fun onCloseClicked_textAdded_then_textCleared(): Unit = with(rule) {
            // setup
            setupUpdateText("a")
            setupClear()
            sut.onWriteClicked()
            sut.onTweetTextChanged("a")

            // exercise
            sut.onCloseClicked()

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isVisible.value).isFalse()
        }

        @Test
        fun onTweetTextChanged_removedText_then_menuItemIsSendDisabled(): Unit = with(rule) {
            // setup
            setupUpdateText("")
            setupUpdateText("a")
            sut.onWriteClicked()

            // exercise
            sut.onTweetTextChanged("a")
            sut.onTweetTextChanged("")

            // verify
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.isVisible.value).isTrue()
        }

        @Test
        fun onSendClicked_whenSendIsSucceeded_then_menuItemIsWriteEnabled(): Unit = with(rule) {
            // setup
            setupUpdateText("a")
            setupPost()
            sut.onWriteClicked()
            sut.onTweetTextChanged("a")

            // exercise
            sut.onSendClicked()

            // verify
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.text.value).isEmpty()
            assertThat(sut.isVisible.value).isFalse()
        }

        @Test
        fun onSendClicked_whenSendIsFailed_then_menuItemIsRetryEnabled(): Unit = with(rule) {
            // setup
            setupUpdateText("a")
            setupPost(withError = AppTwitterException(403, 123))
            sut.onWriteClicked()
            sut.onTweetTextChanged("a")

            // exercise
            sut.onSendClicked()

            // verify
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.RETRY_ENABLED)
            assertThat(sut.text.value).isEqualTo("a")
            assertThat(sut.isVisible.value).isFalse()
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
    private val textFlow = MutableStateFlow("")
    private val repository = MockVerified.create<TweetInputRepository>().apply {
        setupResponseWithVerify({ mock.text }, textFlow)
    }

    val sut: TweetInputViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        TweetInputViewModel(
            eventDispatcher,
            TweetInputViewState(
                collapsible,
                TweetInputActions(eventDispatcher),
                repository.mock,
                AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
            ),
        )
    }

    override fun starting(description: Description?) {
        super.starting(description)
        with(sut) {
            listOf(isVisible, menuItem, text).forEach { it.observeForever { } }
        }
    }

    fun setupUpdateText(text: String) {
        repository.setupResponseWithVerify(
            { repository.mock.updateText(text) },
            Unit,
            alsoOnAnswer = { textFlow.value = text }
        )
    }

    fun setupPost(withError: Throwable? = null) {
        if (withError == null) {
            repository.coSetupResponseWithVerify({ repository.mock.post() }, Unit)
            setupClear()
        } else {
            repository.coSetupThrowWithVerify({ repository.mock.post() }, withError)
        }
    }

    fun setupClear() {
        repository.setupResponseWithVerify(
            { repository.mock.clear() },
            Unit,
            alsoOnAnswer = { textFlow.value = "" }
        )
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(coroutineTestRule)
            .around(InstantTaskExecutorRule())
            .around(repository)
            .apply(super.apply(base, description), description)
    }
}
