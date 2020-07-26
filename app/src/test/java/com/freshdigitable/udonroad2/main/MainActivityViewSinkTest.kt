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

class MainActivityViewSinkTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val dispatcher = NavigationDispatcher()
    private val tokenRepository = mockk<OAuthTokenRepository>()
    private val sut = MainActivityViewSink(
        MainActivityStateModel(
            MainActivityAction(dispatcher),
            tokenRepository,
            SelectedItemRepository()
        )
    )

    @Test
    fun state_dispatchSetupEvent_then_containerStateIsInit() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns null
        sut.state.observeForever {}

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup)

        // verify
        assertThat(sut.state.value?.containerState)
            .isEqualTo(MainNavHostState.Timeline(QueryType.Oauth, MainNavHostState.Cause.INIT))
    }

    @Test
    fun state_dispatchToggleTweetEvent_then_fabVisibleIsTrue() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns 100000L
        every { tokenRepository.login(100000L) } just runs
        sut.state.observeForever {}

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup)
        dispatcher.postEvent(
            TimelineEvent.ToggleTweetItemSelectedState(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), 20000L
                )
            )
        )

        // verify
        assertThat(sut.state.value?.containerState)
            .isEqualTo(
                MainNavHostState.Timeline(
                    QueryType.TweetQueryType.Timeline(),
                    MainNavHostState.Cause.INIT
                )
            )
        assertThat(sut.state.value?.fabVisible).isTrue()
    }
}
