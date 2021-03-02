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
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.TweetInputRepository
import com.freshdigitable.udonroad2.input.MediaChooserResultContract.MediaChooserResult
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.test_common.MatcherScopedBlock
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.AppSettingRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.junit.Before
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
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.user.value).isNotNull()
        }

        @Test
        fun onCancelClicked_whenInputIsCollapsed_menuItemIsNotChanged(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // exercise
                sut.onCancelClicked()
            }

            // verify
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onWriteClicked_then_isVisibleIsTrue(): Unit = with(rule) {
            // exercise
            coroutineTestRule.runBlockingTest {
                sut.onWriteClicked()
            }

            // verify
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.isExpanded.value).isTrue()
        }

        @Test
        fun onCancelClicked_then_isVisibleIsFalse(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // setup
                sut.onWriteClicked()

                // exercise
                sut.onCancelClicked()
            }

            // verify
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
        }

        @Test
        fun onTweetTextChanged_addedText_then_menuItemIsSendEnabled(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // setup
                sut.onWriteClicked()

                // exercise
                sut.onTweetTextChanged(editable("a"))
            }

            // verify
            assertThat(sut.state.value?.text).isEqualTo("a")
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_ENABLED)
            assertThat(sut.isExpanded.value).isTrue()
        }

        @Test
        fun onCancelClicked_textAdded_then_textCleared(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // setup
                sut.onWriteClicked()
                sut.onTweetTextChanged(editable("a"))

                // exercise
                sut.onCancelClicked()
            }

            // verify
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.state.value?.media).isEmpty()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onTweetTextChanged_removedText_then_menuItemIsSendDisabled(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // setup
                sut.onWriteClicked()

                // exercise
                sut.onTweetTextChanged(editable("a"))
                sut.onTweetTextChanged(editable(""))
            }

            // verify
            assertThat(sut.state.value?.text).isEmpty()
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
        fun onMediaChooserFinished_whenCameraAppChosen_then_dispatchAddResult(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            val path = mockk<AppFilePath>()
            coroutineTestRule.runBlockingTest {
                sut.onWriteClicked()
                sut.onCameraAppCandidatesQueried(listOf(cameraApp), path)
                dispatchChosen(cameraApp)

                // exercise
                sut.onMediaChooserFinished(MediaChooserResult.Add(listOf(path)))
            }

            // verify
            assertThat(actual).hasSize(4)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Selected>()
            assertThat(actual[3]).isInstanceOf<CameraApp.State.Finished>()
            assertThat(sut.state.value?.media).hasSize(1)
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_ENABLED)
        }

        @Test
        fun onCancelClicked_whenHasMedia_then_mediaIsEmpty(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            val path = mockk<AppFilePath>()
            coroutineTestRule.runBlockingTest {
                sut.onCameraAppCandidatesQueried(listOf(cameraApp), path)
                dispatchChosen(cameraApp)
                sut.onMediaChooserFinished(MediaChooserResult.Add(listOf(path)))

                // exercise
                sut.onCancelClicked()
            }

            // verify
            assertThat(actual).hasSize(4)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Selected>()
            assertThat(actual[3]).isInstanceOf<CameraApp.State.Finished>()
            assertThat(sut.state.value?.media).isEmpty()
        }

        @Test
        fun onMediaChooserFinishedAtIdlingState_then_stillIdling(): Unit = with(rule) {
            // setup
            val actual = sut.chooserForCameraApp.testCollect(executor)
            coroutineTestRule.runBlockingTest {
                sut.onCameraAppCandidatesQueried(listOf(cameraApp), mockk())
                dispatchChosen(galleryApp)

                // exercise
                sut.onMediaChooserFinished(MediaChooserResult.Replace(listOf()))
            }

            // verify
            assertThat(actual).hasSize(3)
            assertThat(actual[0]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(actual[1]).isInstanceOf<CameraApp.State.WaitingForChosen>()
            assertThat(actual[2]).isInstanceOf<CameraApp.State.Idling>()
            assertThat(sut.state.value?.media).hasSize(0)
        }

        @Test
        fun onSendClicked_whenSendIsSucceeded_then_menuItemIsWriteEnabled(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // setup
                setupPost("a")
                sut.onWriteClicked()
                sut.onTweetTextChanged(editable("a"))

                // exercise
                sut.onSendClicked()
            }

            // verify
            inputTaskObserver.verifyInputTaskOrderOfOnChanged(
                InputTaskState.IDLING,
                InputTaskState.OPENED,
                InputTaskState.SENDING,
                InputTaskState.SUCCEEDED,
                InputTaskState.IDLING
            )
            assertThat(sut.state.value?.taskState).isEqualTo(InputTaskState.IDLING)
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.state.value?.media).isEmpty()
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun onSendClicked_whenSendIsSucceededWithMedia_then_menuItemIsWriteEnabled(): Unit =
            with(rule) {
                // setup
                val path = mockk<AppFilePath>()
                val mediaId = MediaId(1000)
                setupPost("a", listOf(mediaId))
                setupUploadMedia(path, mediaId)
                coroutineTestRule.runBlockingTest {
                    sut.onWriteClicked()
                    sut.onCameraAppCandidatesQueried(listOf(cameraApp), path)
                    dispatchChosen(cameraApp)
                    sut.onMediaChooserFinished(MediaChooserResult.Add(listOf(path)))
                    sut.onTweetTextChanged(editable("a"))

                    // exercise
                    sut.onSendClicked()
                }

                // verify
                inputTaskObserver.verifyInputTaskOrderOfOnChanged(
                    InputTaskState.IDLING,
                    InputTaskState.OPENED,
                    InputTaskState.SENDING,
                    InputTaskState.SUCCEEDED,
                    InputTaskState.IDLING
                )
                assertThat(sut.state.value?.taskState).isEqualTo(InputTaskState.IDLING)
                assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
                assertThat(sut.state.value?.text).isEmpty()
                assertThat(sut.state.value?.media).isEmpty()
                assertThat(sut.isExpanded.value).isFalse()
            }

        @Test
        fun onSendClicked_whenSendIsFailed_then_menuItemIsRetryEnabled(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                // setup
                setupPost("a", withError = AppTwitterException(403, 123))
                sut.onWriteClicked()
                sut.onTweetTextChanged(editable("a"))

                // exercise
                sut.onSendClicked()
            }

            // verify
            inputTaskObserver.verifyInputTaskOrderOfOnChanged(
                InputTaskState.IDLING,
                InputTaskState.OPENED,
                InputTaskState.SENDING,
                InputTaskState.FAILED
            )
            assertThat(sut.state.value?.taskState).isEqualTo(InputTaskState.FAILED)
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.RETRY_ENABLED)
            assertThat(sut.state.value?.text).isEqualTo("a")
            assertThat(sut.state.value?.media).isEmpty()
            assertThat(sut.isExpanded.value).isFalse()
        }

        @Test
        fun dispatchReply_replyTextIsCreated_then_menuIsSendEnabled(): Unit = with(rule) {
            // setup
            val selectedTweetId = TweetId(2000)
            setupReplyTargetTweet(selectedTweetId, "@user200 ")

            // exercise
            coroutineTestRule.runBlockingTest {
                dispatchReply(selectedTweetId)
            }

            // verify
            assertThat(sut.isExpanded.value).isTrue()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_ENABLED)
            assertThat(sut.state.value?.text).isEqualTo("@user200 ")
        }

        @Test
        fun dispatchReply_replyTextIsEmpty_then_menuIsSendDisabled(): Unit = with(rule) {
            // setup
            val selectedTweetId = TweetId(2000)
            setupReplyTargetTweet(selectedTweetId, "")

            // exercise
            coroutineTestRule.runBlockingTest {
                dispatchReply(selectedTweetId)
            }

            // verify
            assertThat(sut.isExpanded.value).isTrue()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.state.value?.text).isEmpty()
        }

        @Test
        fun dispatchQuote_then_expanded(): Unit = with(rule) {
            // setup
            val selectedTweetId = TweetId(2000)

            // exercise
            coroutineTestRule.runBlockingTest {
                dispatchQuote(selectedTweetId)
            }

            // verify
            assertThat(sut.isExpanded.value).isTrue()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.state.value?.hasQuote).isTrue()
        }
    }

    class WhenReply {
        @get:Rule
        val rule = TweetInputViewModelRule(true)

        private val targetTweetId = TweetId(1000)

        @Before
        fun setup(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                setupReplyTargetTweet(targetTweetId, "@user200 ")
                dispatchReply(targetTweetId)
            }

            assertThat(sut.isExpanded.value).isTrue()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_ENABLED)
            assertThat(sut.state.value?.text).isEqualTo("@user200 ")
            assertThat(sut.state.value?.hasReply).isTrue()
        }

        @Test
        fun onSendClicked_whenTaskIsSucceeded_then_stateIsCleared(): Unit = with(rule) {
            // setup
            setupPost("@user200 ", replyTo = targetTweetId)

            // exercise
            coroutineTestRule.runBlockingTest {
                sut.onSendClicked()
            }

            // verify
            inputTaskObserver.verifyInputTaskOrderOfOnChanged(
                InputTaskState.IDLING,
                InputTaskState.OPENED,
                InputTaskState.SENDING,
                InputTaskState.SUCCEEDED,
                InputTaskState.IDLING
            )
            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.state.value?.hasReply).isFalse()
        }

        @Test
        fun onCancelClicked_then_stateIsCleared(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                sut.onCancelClicked()
            }

            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.state.value?.hasReply).isFalse()
        }
    }

    class WhenQuote {
        @get:Rule
        val rule = TweetInputViewModelRule(collapsible = true)
        private val selectedTweetId = TweetId(2000)

        @Before
        fun setup(): Unit = with(rule) {
            coroutineTestRule.runBlockingTest {
                dispatchQuote(selectedTweetId)
            }

            assertThat(sut.isExpanded.value).isTrue()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.SEND_DISABLED)
            assertThat(sut.state.value?.text).isEmpty()
            assertThat(sut.state.value?.hasReply).isFalse()
            assertThat(sut.state.value?.hasQuote).isTrue()
        }

        @Test
        fun onCancelClicked(): Unit = with(rule) {
            // exercise
            coroutineTestRule.runBlockingTest {
                sut.onCancelClicked()
            }

            // verify
            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.state.value?.hasQuote).isFalse()
        }

        @Test
        fun onSendClicked(): Unit = with(rule) {
            // setup
            setupTweet(selectedTweetId, "https://twitter.com/user200/status/2000")
            setupPost(text = { any() })

            // exercise
            coroutineTestRule.runBlockingTest {
                sut.onTweetTextChanged(editable("aaa"))
                sut.onSendClicked()
            }

            // verify
            inputTaskObserver.verifyInputTaskOrderOfOnChanged(
                InputTaskState.IDLING,
                InputTaskState.OPENED,
                InputTaskState.SENDING,
                InputTaskState.SUCCEEDED,
                InputTaskState.IDLING
            )
            verifyPost("aaa https://twitter.com/user200/status/2000")
            assertThat(sut.isExpanded.value).isFalse()
            assertThat(sut.menuItem.value).isEqualTo(InputMenuItem.WRITE_ENABLED)
            assertThat(sut.state.value?.hasQuote).isFalse()
        }
    }

    class WhenCollapsibleIsFalse {
        @get:Rule
        val rule = TweetInputViewModelRule(collapsible = false)

        @Test
        fun initialValue(): Unit = with(rule) {
            // verify
            assertThat(sut).isNotNull()
            assertThat(sut.state.value?.text).isEmpty()
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
    val coroutineTestRule = CoroutineTestRule()
    private val repository = MockVerified.create<TweetInputRepository>()
    val inputTaskObserver: Observer<InputViewState> = spyk<Observer<InputViewState>>().apply {
        every { onChanged(any()) } just runs
    }
    private val appSettingRepositoryRule = AppSettingRepositoryRule()
    private val userRepositoryRule = MockVerified.create<UserDataSource>()
    private val createReplyTextUseCaseRule = MockVerified.create<CreateReplyTextUseCase>()
    private val createQuoteTextUseCaseRule = MockVerified.create<CreateQuoteTextUseCase>()
    val executor = AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
    private val eventDispatcher = EventDispatcher()

    internal val sut: TweetInputViewModel by lazy {
        TweetInputViewModel(
            eventDispatcher,
            TweetInputViewModelSource(
                collapsible,
                TweetInputActions(eventDispatcher),
                createReplyTextUseCaseRule.mock,
                createQuoteTextUseCaseRule.mock,
                TweetInputSharedState(),
                repository.mock,
                appSettingRepositoryRule.mock,
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
        val authenticatedUser = mockk<UserEntity>().apply {
            every { id } returns userId
            every { name } returns "user1"
            every { screenName } returns "User1"
        }
        appSettingRepositoryRule.setupCurrentUserIdSource(executor, userId.value)
        userRepositoryRule.setupResponseWithVerify(
            { userRepositoryRule.mock.getUserSource(userId) },
            flow { emit(authenticatedUser) }
        )
        with(sut) {
            state.observeForever(inputTaskObserver)
            listOf(
                isExpanded, menuItem, user
            ).forEach { it.observeForever { } }
        }
    }

    fun setupPost(
        text: String,
        mediaIds: List<MediaId> = emptyList(),
        replyTo: TweetId? = null,
        withError: Throwable? = null
    ) {
        when (withError) {
            null -> repository.coSetupResponseWithVerify(
                { repository.mock.post(text, mediaIds, replyTo) }, mockk()
            )
            else -> repository.coSetupThrowWithVerify(
                { repository.mock.post(text, mediaIds, replyTo) }, withError
            )
        }
    }

    fun setupPost(text: MatcherScopedBlock<String>) {
        repository.coSetupResponseWithVerify(
            { repository.mock.post(text(), any(), any()) }, mockk()
        )
    }

    fun verifyPost(text: String) {
        coVerify { repository.mock.post(text, any(), any()) }
    }

    fun setupUploadMedia(path: AppFilePath, res: MediaId) {
        repository.coSetupResponseWithVerify({ repository.mock.uploadMedia(path) }, res)
    }

    fun setupReplyTargetTweet(targetTweetId: TweetId, response: String) {
        createReplyTextUseCaseRule.coSetupResponseWithVerify(
            target = { createReplyTextUseCaseRule.mock(targetTweetId) },
            res = response
        )
    }

    fun Observer<InputViewState>.verifyInputTaskOrderOfOnChanged(vararg state: InputTaskState) {
        verifyOrder {
            state.forEach { s -> onChanged(match { it.taskState == s }) }
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
            .around(appSettingRepositoryRule)
            .around(userRepositoryRule)
            .around(createReplyTextUseCaseRule)
            .apply(super.apply(base, description), description)
    }

    fun dispatchChosen(app: Components) {
        eventDispatcher.postEvent(CameraApp.Event.Chosen(app))
    }

    fun dispatchReply(tweetId: TweetId) {
        eventDispatcher.postEvent(SelectedItemShortcut.Reply(tweetId))
    }

    fun dispatchQuote(tweetId: TweetId) {
        eventDispatcher.postEvent(SelectedItemShortcut.Quote(tweetId))
    }

    fun setupTweet(id: TweetId, s: String) {
        createQuoteTextUseCaseRule.coSetupResponseWithVerify(
            target = { createQuoteTextUseCaseRule.mock(id) },
            res = s
        )
    }
}

inline fun <reified T> Subject.isInstanceOf() {
    this.isInstanceOf(T::class.java)
}
