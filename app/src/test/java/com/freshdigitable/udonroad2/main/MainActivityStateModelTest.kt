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

import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Test

class MainActivityStateModelTest {
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
        val testContainerState = sut.containerState.test()
        val testTitle = sut.title.test()
        val testSelectedItemId = sut.selectedItemId.test()

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup)

        // verify
        testContainerState.assertOf {
            it.assertValueSequenceOnly(listOf(MainActivityState.Init(QueryType.Oauth)))
            it.assertNotComplete()
        }
        testTitle.assertOf {
            it.assertValue("Welcome")
            it.assertNotComplete()
        }
        testSelectedItemId.assertOf {
            it.assertValue { actual -> actual.value?.originalId == null }
        }
    }

    @Test
    fun fabVisible() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns 10000
        every { tokenRepository.login(10000) } just runs
        val testContainerState = sut.containerState.test()
        val testTitle = sut.title.test()
        val testSelectedItemId = sut.selectedItemId.test()
        val testFabVisible = sut.isFabVisible.test()

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
        testFabVisible.assertOf {
            it.assertNotComplete()
            it.assertValueSequenceOnly(listOf(false, true))
        }
        testContainerState.assertOf {
            it.assertNotComplete()
            it.assertValueSequenceOnly(listOf(MainActivityState.Init(QueryType.TweetQueryType.Timeline())))
        }
        testTitle.assertOf {
            it.assertNotComplete()
            it.assertValue("Home")
        }
        testSelectedItemId.assertOf {
            it.assertNotComplete()
            it.assertValueCount(2)
            it.assertValueAt(0) { actual -> actual.value?.originalId == null }
            it.assertValueAt(1) { actual -> actual.value?.originalId == 200L }
        }
    }
}
