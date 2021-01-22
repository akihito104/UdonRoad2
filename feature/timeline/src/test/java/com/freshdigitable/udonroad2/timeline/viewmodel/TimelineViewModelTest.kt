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

import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineViewStatesTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TimelineViewModelTest {
    @get:Rule
    val viewStatesTestRule = TimelineViewStatesTestRule()

    @get:Rule
    val repositoryRule = MockVerified.create<ListRepository<QueryType.TweetQueryType>>()

    @get:Rule
    val listProviderRule =
        MockVerified.create<PagedListProvider<QueryType.TweetQueryType, TweetListItem>>().apply {
            val owner = viewStatesTestRule.owner as ListOwner<QueryType.TweetQueryType>
            setupResponseWithVerify(
                { mock.getList(owner.query, owner.id) },
                emptyFlow()
            )
        }

    val sut: TimelineViewModel by lazy {
        TimelineViewModel(
            viewStatesTestRule.owner as ListOwner<QueryType.TweetQueryType>,
            viewStatesTestRule.actionsRule.dispatcher,
            viewStatesTestRule.sut,
            repositoryRule.mock,
            listProviderRule.mock,
        )
    }

    private lateinit var isHeadingEnabledFlow: List<Boolean>
    private lateinit var navigationEvents: List<NavigationEvent>

    @Before
    fun setup() {
        isHeadingEnabledFlow = sut.isHeadingEnabled.testCollect(viewStatesTestRule.executor)
        navigationEvents = sut.navigationEvent.testCollect(viewStatesTestRule.executor)
        sut.timeline.testCollect(viewStatesTestRule.executor)
        sut.selectedItemId.observeForever { }
        sut.feedbackMessage.testCollect(viewStatesTestRule.executor)
    }

    @Test
    fun init() {
        assertThat(sut.selectedItemId.value).isNull()
        assertThat(isHeadingEnabledFlow.first()).isFalse()
    }

    @Test
    fun onListScrollStarted_then_isHeadingEnabledIsTrue() {
        viewStatesTestRule.coroutineTestRule.runBlockingTest {
            // exercise
            sut.onListScrollStarted()
        }

        // verify
        assertThat(isHeadingEnabledFlow.last()).isTrue()
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
        assertThat(isHeadingEnabledFlow.last()).isTrue()
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
            val item = mockk<TweetListItem>().also {
                every { it.originalId } returns TweetId(1000)
                every { it.body } returns mockk(relaxed = true)
            }
            sut.onListScrollStopped(0)

            // exercise
            sut.onBodyItemClicked(item)
        }

        // verify
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(1000))
        assertThat(isHeadingEnabledFlow.last()).isTrue()
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
            val item = mockk<TweetListItem>().also {
                every { it.originalId } returns TweetId(1000)
                every { it.body } returns mockk(relaxed = true)
            }
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
}
