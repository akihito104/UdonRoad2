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

package com.freshdigitable.udonroad2.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.test.MockVerified
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainActivityStateModelTest {
    @get:Rule
    val rule = MainActivityStateModelTestRule()

    @Test
    fun containerState_dispatchSetupEvent_then_showOauth(): Unit = with(rule) {
        // setup
        setupCurrentUserId(null)

        // exercise
        dispatchEvents(TimelineEvent.Setup())

        // verify
        val actualContainerState = sut.containerState.value as MainNavHostState.Timeline
        assertThat(actualContainerState.owner.query).isEqualTo(QueryType.Oauth)
        assertThat(actualContainerState.cause).isEqualTo(MainNavHostState.Cause.INIT)
        assertThat(sut.selectedItemId.value).isEqualTo(null)
    }

    @Test
    fun fabVisible_dispatchToggleSelectedItemEvent_then_fabVisibleIsTrue(): Unit = with(rule) {
        // setup
        setupCurrentUserId(10000)

        // exercise
        dispatchEvents(
            TimelineEvent.Setup(),
            TimelineEvent.ToggleTweetItemSelectedState(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), 200
                )
            )
        )

        // verify
        assertThat(sut.isFabVisible.value).isTrue()
        val actualContainerState = sut.containerState.value as MainNavHostState.Timeline
        assertThat(actualContainerState.owner.query).isEqualTo(QueryType.TweetQueryType.Timeline())
        assertThat(actualContainerState.cause).isEqualTo(MainNavHostState.Cause.INIT)
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(200L)
    }

    @Test
    fun selectedItemId_dispatchMediaItemClickedEvent_then_selectedItemIdHasValue(): Unit =
        with(rule) {
            // setup
            setupCurrentUserId(10000)

            // exercise
            dispatchEvents(
                TimelineEvent.Setup(),
                TimelineEvent.MediaItemClicked(
                    1000,
                    0,
                    SelectedItemId(ListOwner(0, QueryType.TweetQueryType.Timeline()), 1000)
                )
            )

            // verify
            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(1000L)
        }
}

class MainActivityStateModelTestRule : TestWatcher() {
    private val dispatcher = NavigationDispatcher()
    private val tokenRepository = mockk<OAuthTokenRepository>()
    val mockVerified = MockVerified(listOf(tokenRepository))
    val sut = MainActivityStateModel(
        MainActivityAction(dispatcher),
        tokenRepository,
        SelectedItemRepository()
    )

    fun setupCurrentUserId(userId: Long?) {
        every { tokenRepository.getCurrentUserId() } returns userId
        mockVerified.expected { verify { tokenRepository.getCurrentUserId() } }
        if (userId != null) {
            every { tokenRepository.login(userId) } just runs
            mockVerified.expected { verify { tokenRepository.login(userId) } }
        }
    }

    fun dispatchEvents(vararg events: NavigationEvent) {
        events.forEach(dispatcher::postEvent)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        listOf(
            sut.containerState,
            sut.isFabVisible,
            sut.selectedItemId
        ).forEach { it.observeForever {} }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(mockVerified)
            .apply(super.apply(base, description), description)
    }
}
