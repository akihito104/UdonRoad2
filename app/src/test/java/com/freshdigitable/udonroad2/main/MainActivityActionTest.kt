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
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Test

class MainActivityActionTest {
    private val navigationDispatcher = NavigationDispatcher()
    private val tokenRepository: OAuthTokenRepository = mockk()
    private val mainActivityAction = MainActivityAction(navigationDispatcher, tokenRepository)

    @Test
    fun updateContainer_dispatchSetupEvent_then_flowInitOauthEvent() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns null
        val test = mainActivityAction.updateContainer.test()

        // exercise
        navigationDispatcher.postEvent(TimelineEvent.Setup())

        // verify
        test.assertOf {
            it.assertNoErrors()
            it.assertValueCount(1)
            it.assertValueAt(0) { actual ->
                actual is MainNavHostState.Timeline &&
                    actual.owner.query is QueryType.Oauth &&
                    actual.cause == MainNavHostState.Cause.INIT
            }
        }
    }

    @Test
    fun updateContainer_dispatchSetupEvent_then_TimelineQueryIsFlowing() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns UserId(10000)
        every { tokenRepository.login(UserId(10000)) } just runs
        val test = mainActivityAction.updateContainer.test()

        // exercise
        navigationDispatcher.postEvent(TimelineEvent.Setup())

        // verify
        test.assertOf {
            it.assertNoErrors()
            it.assertValueCount(1)
            it.assertValueAt(0) { actual ->
                actual is MainNavHostState.Timeline &&
                    actual.owner.query is QueryType.TweetQueryType.Timeline &&
                    actual.cause == MainNavHostState.Cause.INIT
            }
        }
    }

    @Test
    fun updateContainer_dispatchOauthSucceededEvent_then_InitTimelineEventIsFlowing() {
        // setup
        every { tokenRepository.getCurrentUserId() } returns UserId(10000)
        every { tokenRepository.login(UserId(10000)) } just runs
        val test = mainActivityAction.updateContainer.test()

        // exercise
        navigationDispatcher.postEvent(OauthEvent.OauthSucceeded)

        // verify
        test.assertOf {
            it.assertNoErrors()
            it.assertValueCount(1)
            it.assertValueAt(0) { actual ->
                actual is MainNavHostState.Timeline &&
                    actual.owner.query is QueryType.TweetQueryType.Timeline &&
                    actual.cause == MainNavHostState.Cause.INIT
            }
        }
    }

    @Test
    fun dispatch2Events() {
        every { tokenRepository.getCurrentUserId() } returns UserId(10000)
        every { tokenRepository.login(UserId(10000)) } just runs
        val testShowFirstView = mainActivityAction.updateContainer.test()
        val testToggleSelectedItem = mainActivityAction.toggleSelectedItem.test()

        navigationDispatcher.postEvent(TimelineEvent.Setup())
        navigationDispatcher.postEvent(
            TimelineEvent.ToggleTweetItemSelectedState(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), null
                )
            )
        )

        testShowFirstView.assertOf {
            it.assertNotComplete()
            it.assertValueCount(1)
        }
        testToggleSelectedItem.assertOf {
            it.assertNotComplete()
            it.assertValueCount(1)
        }
    }
}
