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

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class TimelineActionsTest {
    @get:Rule
    val rule = TimelineActionsTestRule()

    @Test
    fun favTweet_dispatchLikeEvent_then_LikeEventDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            SelectedItemShortcut.Like(TweetId(1000))
        )

        // verify
        assertThat(favTweetObserver).containsExactly(SelectedItemShortcut.Like(TweetId(1000)))
    }

    @Test
    fun retweet_dispatchRetweetEvent_then_RetweetEventDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        assertThat(retweetObserver).containsExactly(SelectedItemShortcut.Retweet(TweetId(1000)))
    }

    @Test
    fun dispatchLikeAndRetweetEvent_then_2EventsDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            SelectedItemShortcut.Like(TweetId(1000)),
            SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        assertThat(favTweetObserver).containsExactly(SelectedItemShortcut.Like(TweetId(1000)))
        assertThat(retweetObserver).containsExactly(SelectedItemShortcut.Retweet(TweetId(1000)))
    }
}

class TimelineActionsTestRule(
    coroutineRule: CoroutineTestRule = CoroutineTestRule(),
) : TestWatcher() {
    private val eventCollector = ObserverEventCollector(coroutineRule)
    val owner = ListOwner(1, QueryType.TweetQueryType.Timeline())
    val dispatcher = EventDispatcher()
    private val actions = ListItemLoadableActions(owner, dispatcher)
    internal val sut: TimelineActions by lazy {
        TimelineActions(owner, dispatcher, actions, LaunchMediaViewerAction(dispatcher))
    }
    val favTweetObserver: List<SelectedItemShortcut.Like>
        get() = eventCollector.nonNullEventsOf(sut.favTweet)
    val retweetObserver: List<SelectedItemShortcut.Retweet>
        get() = eventCollector.nonNullEventsOf(sut.retweet)

    override fun starting(description: Description?) {
        super.starting(description)
        eventCollector.setupForActivate {
            addAll(sut.favTweet, sut.retweet)
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        val stmt = super.apply(base, description)
        return eventCollector.apply(stmt, description)
    }
}
