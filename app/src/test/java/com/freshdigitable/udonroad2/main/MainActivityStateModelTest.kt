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
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Rule
import org.junit.Test

class MainActivityStateModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val dispatcher = NavigationDispatcher()
    private val tokenRepository = mockk<OAuthTokenRepository>()
    private val sut = MainActivityStateModel(
        MainActivityAction(dispatcher),
        tokenRepository,
        SelectedItemRepository()
    )

    @Test
    fun containerState_dispatchSetupEvent_then_showOauth() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns null
        sut.containerState.observeForever {}
        sut.selectedItemId.observeForever {}

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup)

        // verify
        val actualContainerState = sut.containerState.value as MainNavHostState.Timeline
        assertThat(actualContainerState.owner.query).isEqualTo(QueryType.Oauth)
        assertThat(actualContainerState.cause).isEqualTo(MainNavHostState.Cause.INIT)
        assertThat(sut.selectedItemId.value).isEqualTo(null)
    }

    @Test
    fun fabVisible() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns 10000
        every { tokenRepository.login(10000) } just runs
        sut.containerState.observeForever { }
        sut.selectedItemId.observeForever { }
        sut.isFabVisible.observeForever { }

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup)
        dispatcher.postEvent(
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
}
