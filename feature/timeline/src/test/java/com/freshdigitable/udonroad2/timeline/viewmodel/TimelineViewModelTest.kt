/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.timeline.viewmodel

import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.freshdigitable.udonroad2.timeline.TimelineViewStatesTestRule
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class TimelineViewModelTest {
    private val eventCollector = ObserverEventCollector()
    private val viewStatesTestRule = TimelineViewStatesTestRule(isStateCollected = false)

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(eventCollector)
        .around(viewStatesTestRule)

    internal val sut: TimelineViewModel by lazy {
        TimelineViewModel(
            viewStatesTestRule.sut,
            UserIconViewModelSource(UserIconClickedAction(viewStatesTestRule.dispatcher))
        )
    }

    private val isHeadingEnabledFlow: List<Boolean>
        get() = eventCollector.nonNullEventsOf(sut.listState).map { it.isHeadingEnabled }
    private val navigationEvents: List<AppEffect>
        get() = eventCollector.nonNullEventsOf(sut.effect)

    @Before
    fun setup() {
        eventCollector.setupForActivate {
            addAll(sut.listState, sut.tweetListState, sut.mediaState)
            addAll(sut.timeline)
            addActivityEventStream(sut)
        }
    }

    @Test
    fun init() {
        assertThat(sut.tweetListState.value?.selectedItemId).isNull()
        assertThat(sut.listState.value?.isHeadingEnabled).isFalse()
        assertThat(sut.mediaState.value?.isPossiblySensitiveHidden).isTrue()
    }

    @Test
    fun onListScrollStarted_then_isHeadingEnabledIsTrue() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // exercise
            sut.scrollList.dispatch()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onListScrollStopped_stoppedAt5_then_isHeadingEnabledIsTrue() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.scrollList.dispatch()

            // exercise
            sut.stopScrollingList.dispatch(5)
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onListScrollStopped_stoppedAt0_then_isHeadingEnabledIsFalse() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.scrollList.dispatch()
            sut.stopScrollingList.dispatch(5)
            sut.scrollList.dispatch()

            // exercise
            sut.stopScrollingList.dispatch(0)
        }

        // verify
        assertThat(isHeadingEnabledFlow).containsExactly(false, true, false)
    }

    @Test
    fun onBodyClicked() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            val item = TweetListItem.createMock(
                originalTweetId = TweetId(1000),
                body = mockk(relaxed = true)
            )
            sut.stopScrollingList.dispatch(0)

            // exercise
            sut.selectBodyItem.dispatch(item)
        }

        // verify
        assertThat(sut.tweetListState.value?.selectedItemId?.originalId).isEqualTo(TweetId(1000))
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onHeadingClicked_firstVisibleItemPositionIs0_navigationEventIsEmpty() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.stopScrollingList.dispatch(0)

            // exercise
            sut.heading.dispatch()
        }

        // verify
        assertThat(navigationEvents).isEmpty()
    }

    @Test
    fun onHeadingClicked_clearSelectedItem() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.stopScrollingList.dispatch(0)
            val item = TweetListItem.createMock(
                originalTweetId = TweetId(1000),
                body = mockk(relaxed = true)
            )
            sut.selectBodyItem.dispatch(item)

            // exercise
            sut.heading.dispatch()
        }

        // verify
        assertThat(navigationEvents).isEmpty()
        assertThat(sut.tweetListState.value?.selectedItemId).isNull()
    }

    @Test
    fun onHeadingClicked_firstVisibleItemPositionIs3_ToTopOfListWithNeedsSkipIsFalseIsCollected() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.stopScrollingList.dispatch(3)

            // exercise
            sut.heading.dispatch()
        }

        // verify
        assertThat(navigationEvents.last()).isEqualTo(TimelineEffect.ToTopOfList(false))
    }

    @Test
    fun onHeadingClicked_firstVisibleItemPositionIs3_ToTopOfListWithNeedsSkipIsTrueIsCollected() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.stopScrollingList.dispatch(4)

            // exercise
            sut.heading.dispatch()
        }

        // verify
        assertThat(navigationEvents.last()).isEqualTo(TimelineEffect.ToTopOfList(true))
    }

    @Test
    fun onRefresh_prependListReturns0Items_then_isHeadingEnabledIsFalse() {
        // setup
        viewStatesTestRule.setupPrependListResponse()

        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            sut.stopScrollingList.dispatch(0)

            // exercise
            sut.prependList.dispatch()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isFalse()
    }

    @Test
    fun prependListReturns0ItemsWhenFirstVisiblePositionIs1_then_isHeadingEnabledIsTrue() {
        // setup
        viewStatesTestRule.setupPrependListResponse()

        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            sut.stopScrollingList.dispatch(1)

            // exercise
            sut.prependList.dispatch()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onRefresh_prependListReturns1Items_then_isHeadingEnabledIsTrue() {
        // setup
        viewStatesTestRule.setupPrependListResponse(
            mockk<List<TweetEntity>>().also {
                every { it.size } returns 1
            }
        )

        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            sut.stopScrollingList.dispatch(0)

            // exercise
            sut.prependList.dispatch()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }
}
