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
import androidx.lifecycle.Observer
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun state() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns null
        val stateObserver = mockk<Observer<MainActivityViewState>>(relaxed = true)
        sut.state.observeForever(stateObserver)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup)

        // verify
        verify {
            stateObserver.onChanged(match {
                it.containerState == MainActivityState.Init(QueryType.Oauth)
            })
        }
    }
}
