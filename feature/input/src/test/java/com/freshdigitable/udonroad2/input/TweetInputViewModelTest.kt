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
import androidx.lifecycle.Observer
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.ExpectedException
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
            assertThat(sut.user.value).isNotNull()
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
        fun onCameraAppCandidatesQueried(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)

            // exercise
            sut.onCameraAppCandidatesQueried(listOf(cameraApp), mockk())

            // verify
            assertThat(actual).hasSize(2)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
        }

        @Test
        fun dispatchChosenWithCameraApp_then_dispatchSelectedEvent(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            sut.onCameraAppCandidatesQueried(listOf(cameraApp), mockk())

            // exercise
            dispatchChosen(cameraApp)

            // verify
            assertThat(actual).hasSize(3)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Selected>()
        }

        @Test
        fun dispatchChosenWithNotCameraApp_then_moveIdlingState(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            sut.onCameraAppCandidatesQueried(listOf(cameraApp), mockk())

            // exercise
            dispatchChosen(galleryApp)

            // verify
            assertThat(actual).hasSize(3)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Idling>()
        }

        @Test
        fun dispatchChosenNotAtCandidatesQueried_then_throwException(): Unit = with(rule) {
            // setup
            sut.chooserForCameraApp.testCollect(executor)
            expectedException.expect(RuntimeException::class.java)

            // exercise
            dispatchChosen(galleryApp)
        }

        @Test
        fun onCameraAppFinished(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            sut.onCameraAppCandidatesQueried(listOf(cameraApp), mockk())
            dispatchChosen(cameraApp)

            // exercise
            sut.onCameraAppFinished()

            // verify
            assertThat(actual).hasSize(4)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Selected>()
            assertThat(actual[3]).isInstanceOf<CameraApp.State.Finished>()
        }

        @Test
        fun onCameraAppFinishedAtIdlingState_then_stillIdling(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            sut.onCameraAppCandidatesQueried(listOf(cameraApp), mockk())
            dispatchChosen(galleryApp)

            // exercise
            sut.onCameraAppFinished()

            // verify
            assertThat(actual).hasSize(3)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Idling>()
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
            inputTaskObserver.verifyOrderOfOnChanged(
                InputTaskState.IDLING,
                InputTaskState.OPENED,
                InputTaskState.SENDING,
                InputTaskState.SUCCEEDED,
                InputTaskState.IDLING
            )
            assertThat(sut.inputTask.value).isEqualTo(InputTaskState.IDLING)
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
            inputTaskObserver.verifyOrderOfOnChanged(
                InputTaskState.IDLING,
                InputTaskState.OPENED,
                InputTaskState.SENDING,
                InputTaskState.FAILED
            )
            assertThat(sut.inputTask.value).isEqualTo(InputTaskState.FAILED)
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
    val expectedException: ExpectedException = ExpectedException.none()
    private val coroutineTestRule = CoroutineTestRule()
    private val repository = MockVerified.create<TweetInputRepository>()
    val inputTaskObserver: Observer<InputTaskState> = spyk<Observer<InputTaskState>>().apply {
        every { onChanged(any()) } just runs
    }
    private val oAuthTokenRepositoryRule = OAuthTokenRepositoryRule()
    private val userRepositoryRule: MockVerified<UserRepository> = MockVerified.create()
    val executor = AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
    private val eventDispatcher = EventDispatcher()

    val sut: TweetInputViewModel by lazy {
        TweetInputViewModel(
            eventDispatcher,
            TweetInputViewState(
                collapsible,
                TweetInputActions(eventDispatcher),
                TweetInputSharedState(),
                repository.mock,
                oAuthTokenRepositoryRule.mock,
                userRepositoryRule.mock,
                executor,
            ),
        )
    }

    val cameraApp = Components("com.example", "com.example.ExCameraActivity")
    val galleryApp = Components("com.example", "com.example.ExGalleryActivity")

    override fun starting(description: Description?) {
        super.starting(description)
        val userId = UserId(1000)
        val authenticatedUser = mockk<User>().apply {
            every { id } returns userId
            every { name } returns "user1"
            every { screenName } returns "User1"
        }
        oAuthTokenRepositoryRule.setupCurrentUserIdSource(userId.value)
        userRepositoryRule.setupResponseWithVerify(
            { userRepositoryRule.mock.getUserFlow(userId) },
            flow { emit(authenticatedUser) }
        )
        with(sut) {
            inputTask.observeForever(inputTaskObserver)
            listOf(isExpanded, menuItem, text, media, user).forEach { it.observeForever { } }
        }
    }

    fun setupPost(text: String, withError: Throwable? = null) {
        if (withError == null) {
            repository.coSetupResponseWithVerify({ repository.mock.post(text) }, Unit)
        } else {
            repository.coSetupThrowWithVerify({ repository.mock.post(text) }, withError)
        }
    }

    fun Observer<InputTaskState>.verifyOrderOfOnChanged(vararg state: InputTaskState) {
        verifyOrder {
            state.forEach { onChanged(it) }
        }
    }

    fun editable(text: String): Editable = mockk<Editable>().apply {
        every { this@apply.toString() } returns text
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(expectedException)
            .around(coroutineTestRule)
            .around(InstantTaskExecutorRule())
            .around(repository)
            .around(oAuthTokenRepositoryRule)
            .around(userRepositoryRule)
            .apply(super.apply(base, description), description)
    }

    fun dispatchChosen(app: Components) {
        eventDispatcher.postEvent(CameraApp.Event.Chosen(app))
    }
}

fun <T> Flow<T>.testCollect(executor: AppExecutor): List<T> {
    val actual = mutableListOf<T>()
    executor.launch(executor.dispatcher.mainContext) {
        collect { actual.add(it) }
    }
    return actual
}

inline fun <reified T> Subject.isInstanceOf() {
    this.isInstanceOf(T::class.java)
}
