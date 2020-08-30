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
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TimelineViewStateTest {
    @get:Rule
    val rule = TimelineViewStatesTestRule()

    @Test
    fun selectedItemId_dispatchMediaItemClickedEvent_then_selectedItemIdHasValue(): Unit =
        with(rule) {
            // exercise
            dispatchEvents(
                TimelineEvent.Setup(),
                TimelineEvent.MediaItemClicked(
                    TweetId(1000), 0, SelectedItemId(owner, TweetId(1000))
                )
            )

            // verify
            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(1000L))
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
}

class TimelineViewStatesTestRule : TestWatcher() {
    private val actionsRule: TimelineActionsTestRule = TimelineActionsTestRule()
    val owner = ListOwnerGenerator().create(QueryType.TweetQueryType.Timeline())
    val sut = TimelineViewState(
        owner,
        actionsRule.sut,
        SelectedItemRepository(),
        mockk(),
        ListOwnerGenerator(),
        TimelineNavigationDelegate(mockk(relaxed = true), mockk())
    )

    fun dispatchEvents(vararg event: AppEvent) {
        actionsRule.dispatcher.postEvents(*event)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        sut.selectedItemId.observeForever { }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .apply(super.apply(base, description), description)
    }
}
