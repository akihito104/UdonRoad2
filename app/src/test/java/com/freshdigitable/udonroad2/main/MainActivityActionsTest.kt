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

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.oauth.OauthAction
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.reactivex.observers.TestObserver
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainActivityActionsTest {
    @get:Rule
    val rule = MainActivityActionsTestRule()

    @Test
    fun updateContainer_dispatchSetupEvent_then_flowInitOauthEvent(): Unit = with(rule) {
        // setup
        oauthTokenRepositoryMock.setupCurrentUserId(null)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        updateContainerObserver.assertOf {
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
    fun updateContainer_dispatchSetupEvent_then_TimelineQueryIsFlowing(): Unit = with(rule) {
        // setup
        oauthTokenRepositoryMock.setupCurrentUserId(10000)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        updateContainerObserver.assertOf {
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
    fun updateContainer_dispatchOauthSucceededEvent_then_InitTimelineEventIsFlowing(): Unit =
        with(rule) {
            // setup
            oauthTokenRepositoryMock.setupCurrentUserId(10000)

            // exercise
            dispatcher.postEvent(OauthEvent.OauthSucceeded)

            // verify
            updateContainerObserver.assertOf {
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
    fun dispatch2Events(): Unit = with(rule) {
        // setup
        oauthTokenRepositoryMock.setupCurrentUserId(10000)

        // exercise
        dispatcher.postEvents(
            TimelineEvent.Setup(),
            TimelineEvent.TweetItemSelection.Toggle(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(100)
                )
            )
        )

        // verify
        updateContainerObserver.assertOf {
            it.assertNotComplete()
            it.assertValueCount(1)
        }
        toggleItemObserver.assertOf {
            it.assertNotComplete()
            it.assertValueCount(1)
        }
    }

    @Test
    fun updateTweet_dispatchLikeEvent_then_LikeEventDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            TimelineEvent.SelectedItemShortcut.Like(TweetId(1000))
        )

        // verify
        updateTweetObserver.assertOf {
            it.assertNotComplete()
            it.assertValueCount(1)
            it.assertValueAt(0, TimelineEvent.SelectedItemShortcut.Like(TweetId(1000)))
        }
    }

    @Test
    fun updateTweet_dispatchRetweetEvent_then_RetweetEventDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            TimelineEvent.SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        updateTweetObserver.assertOf {
            it.assertNotComplete()
            it.assertValueCount(1)
            it.assertValueAt(0, TimelineEvent.SelectedItemShortcut.Retweet(TweetId(1000)))
        }
    }

    @Test
    fun updateTweet_dispatchLikeAndRetweetEvent_then_2EventsDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            TimelineEvent.SelectedItemShortcut.Like(TweetId(1000)),
            TimelineEvent.SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        updateTweetObserver.assertOf {
            it.assertNotComplete()
            it.assertValueCount(2)
            it.assertValueAt(0, TimelineEvent.SelectedItemShortcut.Like(TweetId(1000)))
            it.assertValueAt(1, TimelineEvent.SelectedItemShortcut.Retweet(TweetId(1000)))
        }
    }
}

class MainActivityActionsTestRule : TestWatcher() {
    val dispatcher = EventDispatcher()
    val oauthTokenRepositoryMock = OAuthTokenRepositoryRule()
    val sut: MainActivityActions = MainActivityActions(
        dispatcher,
        oauthTokenRepositoryMock.tokenRepository,
        OauthAction(dispatcher),
        ListOwnerGenerator()
    )
    val updateContainerObserver: TestObserver<MainNavHostState> = sut.updateContainer.test()
    val updateTweetObserver: TestObserver<out TimelineEvent.SelectedItemShortcut> =
        sut.updateTweet.test()
    val toggleItemObserver: TestObserver<TimelineEvent.TweetItemSelection.Toggle> =
        sut.toggleItem.test()

    fun TestObserver<out TimelineEvent.SelectedItemShortcut>.assertValueAt(
        index: Int,
        expected: TimelineEvent.SelectedItemShortcut
    ) {
        assertValueAt(index) { it == expected }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(oauthTokenRepositoryMock)
            .apply(super.apply(base, description), description)
    }
}
