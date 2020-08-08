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
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import org.junit.Rule
import org.junit.Test

class MainActivityActionsTest {
    @get:Rule
    val rule = OAuthTokenRepositoryRule()
    private val eventDispatcher = EventDispatcher()
    private val tokenRepository: OAuthTokenRepository = rule.tokenRepository
    private val sut = MainActivityActions(eventDispatcher, tokenRepository)

    @Test
    fun updateContainer_dispatchSetupEvent_then_flowInitOauthEvent() {
        // setup
        rule.setupCurrentUserId(null)
        val test = sut.updateContainer.test()

        // exercise
        eventDispatcher.postEvent(TimelineEvent.Setup())

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
        rule.setupCurrentUserId(10000)
        val test = sut.updateContainer.test()

        // exercise
        eventDispatcher.postEvent(TimelineEvent.Setup())

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
        rule.setupCurrentUserId(10000)
        val test = sut.updateContainer.test()

        // exercise
        eventDispatcher.postEvent(OauthEvent.OauthSucceeded)

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
        rule.setupCurrentUserId(10000)
        val testShowFirstView = sut.updateContainer.test()
        val testToggleSelectedItem = sut.toggleItem.test()

        eventDispatcher.postEvents(
            TimelineEvent.Setup(),
            TimelineEvent.TweetItemSelection.Toggle(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(100)
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
