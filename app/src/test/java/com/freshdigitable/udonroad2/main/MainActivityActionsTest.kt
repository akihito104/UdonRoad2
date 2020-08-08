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

    @Test
    fun updateTweet_dispatchLikeEvent_then_LikeEventDispatched() {
        // setup
        val test = sut.updateTweet.test()

        // exercise
        eventDispatcher.postEvents(
            TimelineEvent.SelectedItemShortcut.Like(TweetId(1000))
        )

        // verify
        test.assertNotComplete()
        test.assertValueCount(1)
        test.assertValueAt(0) {
            it is TimelineEvent.SelectedItemShortcut.Like &&
                it.tweetId == TweetId(1000)
        }
    }

    @Test
    fun updateTweet_dispatchRetweetEvent_then_RetweetEventDispatched() {
        // setup
        val test = sut.updateTweet.test()

        // exercise
        eventDispatcher.postEvents(
            TimelineEvent.SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        test.assertNotComplete()
        test.assertValueCount(1)
        test.assertValueAt(0) {
            it is TimelineEvent.SelectedItemShortcut.Retweet &&
                it.tweetId == TweetId(1000)
        }
    }

    @Test
    fun updateTweet_dispatchLikeAndRetweetEvent_then_2EventsDispatched() {
        // setup
        val test = sut.updateTweet.test()

        // exercise
        eventDispatcher.postEvents(
            TimelineEvent.SelectedItemShortcut.Like(TweetId(1000)),
            TimelineEvent.SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        test.assertNotComplete()
        test.assertValueCount(2)
        test.assertValueAt(0) {
            it is TimelineEvent.SelectedItemShortcut.Like &&
                it.tweetId == TweetId(1000)
        }
        test.assertValueAt(1) {
            it is TimelineEvent.SelectedItemShortcut.Retweet &&
                it.tweetId == TweetId(1000)
        }
    }
}
