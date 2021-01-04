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

import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import io.reactivex.observers.TestObserver
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher

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
        favTweetObserver
            .assertNotComplete()
            .assertValueCount(1)
            .assertValueAt(0, SelectedItemShortcut.Like(TweetId(1000)))
    }

    @Test
    fun retweet_dispatchRetweetEvent_then_RetweetEventDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        retweetObserver
            .assertNotComplete()
            .assertValueCount(1)
            .assertValueAt(0, SelectedItemShortcut.Retweet(TweetId(1000)))
    }

    @Test
    fun dispatchLikeAndRetweetEvent_then_2EventsDispatched(): Unit = with(rule) {
        // exercise
        dispatcher.postEvents(
            SelectedItemShortcut.Like(TweetId(1000)),
            SelectedItemShortcut.Retweet(TweetId(1000))
        )

        // verify
        favTweetObserver
            .assertNotComplete()
            .assertValueCount(1)
            .assertValueAt(0, SelectedItemShortcut.Like(TweetId(1000)))
        retweetObserver
            .assertNotComplete()
            .assertValueCount(1)
            .assertValueAt(0, SelectedItemShortcut.Retweet(TweetId(1000)))
    }
}

class TimelineActionsTestRule : TestWatcher() {
    val dispatcher = EventDispatcher()
    val sut: TimelineActions = TimelineActions(dispatcher)

    val favTweetObserver: TestObserver<SelectedItemShortcut.Like> = sut.favTweet.test()
    val retweetObserver: TestObserver<SelectedItemShortcut.Retweet> = sut.retweet.test()
}
