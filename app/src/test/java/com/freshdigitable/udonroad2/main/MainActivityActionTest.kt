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
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import org.junit.Test

class MainActivityActionTest {
    private val navigationDispatcher = NavigationDispatcher()
    private val mainActivityAction = MainActivityAction(navigationDispatcher)

    @Test
    fun showFirstView_dispatchSetupEvent_then_flowSetupEvent() {
        // setup
        val test = mainActivityAction.showFirstView.test()

        // exercise
        navigationDispatcher.postEvent(TimelineEvent.Setup)

        // verify
        test.assertOf {
            it.assertNoErrors()
            it.assertValueSequence(listOf(TimelineEvent.Setup))
        }
    }

    @Test
    fun showTimeline_dispatchTimelineInitEvent_then_TimelineQueryIsFlowing() {
        // setup
        val test = mainActivityAction.showTimeline.test()

        // exercise
        navigationDispatcher.postEvent(TimelineEvent.Setup)
        navigationDispatcher.postEvent(TimelineEvent.Init)

        // verify
        test.assertOf {
            it.assertNoErrors()
            it.assertValueSequence(listOf(QueryType.TweetQueryType.Timeline()))
        }
    }

    @Test
    fun showFirstView_dispatchOauthSucceededEvent_then_SetupIsFlowing() {
        // setup
        val test = mainActivityAction.showFirstView.test()

        // exercise
        navigationDispatcher.postEvent(OauthEvent.OauthSucceeded)

        // verify
        test.assertOf {
            it.assertNoErrors()
            it.assertValueSequence(listOf(TimelineEvent.Setup))
        }
    }

    @Test
    fun dispatch2Events() {
        val testShowFirstView = mainActivityAction.showFirstView.test()
        val testToggleSelectedItem = mainActivityAction.toggleSelectedItem.test()

        navigationDispatcher.postEvent(TimelineEvent.Setup)
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
