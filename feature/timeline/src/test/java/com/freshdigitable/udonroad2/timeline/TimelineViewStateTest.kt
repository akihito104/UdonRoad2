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

package com.freshdigitable.udonroad2.timeline

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import com.google.common.truth.Truth.assertThat
import io.reactivex.observers.TestObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
class TimelineViewStateTest {
    @get:Rule
    val rule = TimelineViewStatesTestRule()

    @Test
    fun selectedItemId_dispatchMediaItemClickedEvent_then_selectedItemIdHasValue(): Unit =
        with(rule) {
            // setup

            // exercise
            dispatchEvents(
                TimelineEvent.Setup(),
                TimelineEvent.MediaItemClicked(
                    TweetId(1000), 0, SelectedItemId(owner, TweetId(1000))
                )
            )

            // verify
            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(1000L))
            assertThat(navEvents[0]).isInstanceOf(TimelineEvent.Navigate.MediaViewer::class.java)
        }

    @Test
    fun selectedItemId_dispatchToggleEvent_then_hasItem(): Unit = with(rule) {
        // exercise
        dispatchEvents(
            TweetItemSelection.Toggle(SelectedItemId(owner, TweetId(200)))
        )

        // verify
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
    }

    @Test
    fun selectedItemId_dispatchUnselectEvent_then_hasNoItem(): Unit = with(rule) {
        // exercise
        dispatchEvents(
            TweetItemSelection.Toggle(SelectedItemId(owner, TweetId(200))),
            TweetItemSelection.Unselected(owner)
        )

        // verify
        assertThat(sut.selectedItemId.value).isNull()
    }

    @Test
    fun updateTweet_dispatchLikeIsSuccess_then_likeDispatched(): Unit = with(rule) {
        // setup
        tweetRepositoryMock.setupPostLikeForSuccess(TweetId(200))
        dispatchEvents(
            TweetItemSelection.Selected(SelectedItemId(owner, TweetId(200)))
        )

        // exercise
        dispatchEvents(
            SelectedItemShortcut.Like(TweetId(200))
        )

        // verify
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
        messageEvents.assertValueAt(0) { it.messageRes == R.string.msg_fav_create_success }
    }

    @Test
    fun updateTweet_dispatchLikeIsFailure_then_likeDispatchedWithError(): Unit = with(rule) {
        // setup
        tweetRepositoryMock.setupPostLikeForFailure(
            TweetId(200), AppTwitterException.ErrorType.ALREADY_FAVORITED
        )
        dispatchEvents(
            TweetItemSelection.Selected(SelectedItemId(owner, TweetId(200)))
        )

        // exercise
        dispatchEvents(
            SelectedItemShortcut.Like(TweetId(200))
        )

        // verify
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
        messageEvents.assertValueAt(0) { it.messageRes == R.string.msg_already_fav }
    }

    @Test
    fun updateTweet_dispatchRetweetEvent_then_retweetDispatched(): Unit = with(rule) {
        // setup
        tweetRepositoryMock.setupPostRetweetForSuccess(TweetId(200))
        dispatchEvents(
            TweetItemSelection.Selected(SelectedItemId(owner, TweetId(200)))
        )

        // exercise
        dispatchEvents(
            SelectedItemShortcut.Retweet(TweetId(200))
        )

        // verify
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
        messageEvents.assertValueAt(0) { it.messageRes == R.string.msg_rt_create_success }
    }

    @Test
    fun updateTweet_dispatchRetweetIsFailure_then_retweetResultIsDispatchedWithException(): Unit =
        with(rule) {
            // setup
            tweetRepositoryMock.setupPostRetweetForFailure(
                TweetId(200), AppTwitterException.ErrorType.ALREADY_RETWEETED
            )
            dispatchEvents(
                TweetItemSelection.Selected(SelectedItemId(owner, TweetId(200)))
            )

            // exercise
            dispatchEvents(
                SelectedItemShortcut.Retweet(TweetId(200))
            )

            // verify
            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
            messageEvents.assertValueAt(0) { it.messageRes == R.string.msg_already_rt }
        }
}

@ExperimentalCoroutinesApi
class TimelineViewStatesTestRule : TestWatcher() {
    private val actionsRule: TimelineActionsTestRule = TimelineActionsTestRule()
    val tweetRepositoryMock = TweetRepositoryRule()
    val owner = ListOwner(0, QueryType.TweetQueryType.Timeline())
    private val coroutineTestRule = CoroutineTestRule()
    private val executor = AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
    val sut = TimelineViewState(
        owner,
        actionsRule.sut,
        SelectedItemRepository(),
        tweetRepositoryMock.mock,
        ListOwnerGenerator.create(AtomicInteger(1)),
        executor
    )
    val navEvents: List<TimelineEvent.Navigate> = sut.updateNavHost.testCollect(executor)
    val messageEvents: TestObserver<FeedbackMessage> = sut.updateTweet.test()

    fun dispatchEvents(vararg event: AppEvent) {
        actionsRule.dispatcher.postEvents(*event)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        sut.selectedItemId.observeForever { }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(actionsRule)
            .around(tweetRepositoryMock)
            .around(RxExceptionHandler())
            .around(coroutineTestRule)
            .apply(super.apply(base, description), description)
    }
}
