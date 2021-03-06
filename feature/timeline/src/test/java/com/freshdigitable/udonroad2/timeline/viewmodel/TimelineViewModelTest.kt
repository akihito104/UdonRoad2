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
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineViewStatesTestRule
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description

class TimelineViewModelTest {
    @get:Rule
    val viewStatesTestRule = object : TimelineViewStatesTestRule() {
        override fun starting(description: Description?) = Unit
    }

    internal val sut: TimelineViewModel by lazy {
        val eventDispatcher = viewStatesTestRule.actionsRule.dispatcher
        TimelineViewModel(
            viewStatesTestRule.sut,
            UserIconViewModelSource(UserIconClickedAction(eventDispatcher))
        )
    }

    private lateinit var isHeadingEnabledFlow: List<Boolean>
    private lateinit var navigationEvents: List<NavigationEvent>

    @Before
    fun setup() {
        val isHeadingEnabled = mutableListOf<Boolean>()
        isHeadingEnabledFlow = isHeadingEnabled
        sut.listState.observeForever { isHeadingEnabled.add(it.isHeadingEnabled) }
        navigationEvents = sut.navigationEvent.testCollect(viewStatesTestRule.scope)
        sut.timeline.testCollect(viewStatesTestRule.scope)
        sut.selectedItemId.observeForever { }
        sut.feedbackMessage.testCollect(viewStatesTestRule.scope)
        sut.mediaState.observeForever { }
    }

    @Test
    fun init() {
        assertThat(sut.selectedItemId.value).isNull()
        assertThat(sut.listState.value?.isHeadingEnabled).isFalse()
        assertThat(sut.mediaState.value?.isPossiblySensitiveHidden).isTrue()
    }

    @Test
    fun onListScrollStarted_then_isHeadingEnabledIsTrue() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // exercise
            sut.onListScrollStarted()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onListScrollStopped_stoppedAt5_then_isHeadingEnabledIsTrue() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.onListScrollStarted()

            // exercise
            sut.onListScrollStopped(5)
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onListScrollStopped_stoppedAt0_then_isHeadingEnabledIsFalse() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.onListScrollStarted()
            sut.onListScrollStopped(5)
            sut.onListScrollStarted()

            // exercise
            sut.onListScrollStopped(0)
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
            sut.onListScrollStopped(0)

            // exercise
            sut.onBodyItemClicked(item)
        }

        // verify
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(1000))
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }

    @Test
    fun onHeadingClicked_firstVisibleItemPositionIs0_navigationEventIsEmpty() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.onListScrollStopped(0)

            // exercise
            sut.onHeadingClicked()
        }

        // verify
        assertThat(navigationEvents).isEmpty()
    }

    @Test
    fun onHeadingClicked_clearSelectedItem() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.onListScrollStopped(0)
            val item = TweetListItem.createMock(
                originalTweetId = TweetId(1000),
                body = mockk(relaxed = true)
            )
            sut.onBodyItemClicked(item)

            // exercise
            sut.onHeadingClicked()
        }

        // verify
        assertThat(navigationEvents).isEmpty()
        assertThat(sut.selectedItemId.value).isNull()
    }

    @Test
    fun onHeadingClicked_firstVisibleItemPositionIs3_ToTopOfListWithNeedsSkipIsFalseIsCollected() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.onListScrollStopped(3)

            // exercise
            sut.onHeadingClicked()
        }

        // verify
        assertThat(navigationEvents.last()).isEqualTo(TimelineEvent.Navigate.ToTopOfList(false))
    }

    @Test
    fun onHeadingClicked_firstVisibleItemPositionIs3_ToTopOfListWithNeedsSkipIsTrueIsCollected() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // setup
            sut.onListScrollStopped(4)

            // exercise
            sut.onHeadingClicked()
        }

        // verify
        assertThat(navigationEvents.last()).isEqualTo(TimelineEvent.Navigate.ToTopOfList(true))
    }

    @Test
    fun onRefresh_prependListReturns0Items_then_isHeadingEnabledIsFalse() {
        // setup
        viewStatesTestRule.setupPrependListResponse()

        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            sut.onListScrollStopped(0)

            // exercise
            sut.onRefresh()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isFalse()
    }

    @Test
    fun onRefresh_prependListReturns0ItemsWhenFirstVisiblePositionIs1_then_isHeadingEnabledIsTrue() {
        // setup
        viewStatesTestRule.setupPrependListResponse()

        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            sut.onListScrollStopped(1)

            // exercise
            sut.onRefresh()
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
            sut.onListScrollStopped(0)

            // exercise
            sut.onRefresh()
        }

        // verify
        assertThat(sut.listState.value?.isHeadingEnabled).isTrue()
    }
}
