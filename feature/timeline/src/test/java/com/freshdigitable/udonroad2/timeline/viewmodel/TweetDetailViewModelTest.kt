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

package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.jvm.AppSettingRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.freshdigitable.udonroad2.timeline.LaunchMediaViewerAction
import com.freshdigitable.udonroad2.timeline.TweetMediaViewModelSource
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.io.IOException

class TweetDetailViewModelTest {

    private val exceptions: ExpectedException = ExpectedException.none()
    private val tweetRepositoryRule = TweetRepositoryRule()
    private val appSettingRepositoryRule = AppSettingRepositoryRule()
    private val coroutineRule = CoroutineTestRule()
    private val eventCollector = ObserverEventCollector(coroutineRule)

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(exceptions)
        .around(InstantTaskExecutorRule())
        .around(coroutineRule)
        .around(tweetRepositoryRule)
        .around(appSettingRepositoryRule)
        .around(eventCollector)

    private val tweet = DetailTweetListItem.createMock(TweetId(1000))

    private val sut: TweetDetailViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        TweetDetailViewModel(
            TweetDetailViewStates(
                tweet.originalId,
                TweetDetailActions(eventDispatcher),
                GetTweetDetailItemUseCase(tweetRepositoryRule.mock),
                mockk(),
            ),
            UserIconViewModelSource(UserIconClickedAction(eventDispatcher)),
            TweetMediaViewModelSource.create(
                LaunchMediaViewerAction(eventDispatcher),
                appSettingRepositoryRule.mock,
            ),
            coroutineRule.coroutineContextProvider.mainContext,
        )
    }
    private val tweetSource: Channel<TweetListItem?> = Channel()
    private val navigationEvents: List<AppEffect>
        get() = eventCollector.nonNullEventsOf(sut.effect)

    @Before
    fun setup() {
        tweetRepositoryRule.setupShowTweet(tweet.originalId, tweetSource.receiveAsFlow())
        appSettingRepositoryRule.setupIsPossiblySensitiveHidden()
        eventCollector.setupForActivate {
            addAll(sut.state, sut.mediaState)
            addActivityEventStream(sut)
        }
    }

    @Test
    fun initialState() {
        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.state.value?.tweetItem).isNull()
    }

    @Test
    fun removeObserverOnce() {
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)

        eventCollector.deactivateAll()
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)

        eventCollector.activateAll()

        assertThat(sut.state.value).isNotNull()
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)
    }

    @Test
    fun whenItemIsFound_then_tweetItemHasItem() {
        // exercise
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }

        // verify
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)
    }

    @Test
    fun whenItemIsNotFoundInLocal_then_fetchTweetItem() {
        // setup
        tweetRepositoryRule.setupFindTweetItem(tweet.originalId, tweet)

        // exercise
        coroutineRule.runBlockingTest {
            tweetSource.send(null)
        }

        // verify
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)
    }

    @Test
    fun thrownExceptionWhenFetchTweet_then_recovered() {
        // setup
        tweetRepositoryRule.setupFindTweetItem(tweet.originalId, IOException("target"))

        // exercise
        coroutineRule.runBlockingTest {
            tweetSource.send(null)
        }

        // verify
        assertThat(sut.state.value?.tweetItem).isNull()
    }

    @Test
    fun thrownRuntimeExceptionWhenFetchTweet_then_rethrown() {
        // setup
        val target = RuntimeException("target")
        exceptions.expect(CoreMatchers.isA(target::class.java))
        exceptions.expectMessage(target.message)
        tweetRepositoryRule.setupFindTweetItem(tweet.originalId, target)

        // exercise
        coroutineRule.runBlockingTest {
            tweetSource.send(null)
        }

        // verify
        assertThat(sut.state.value?.tweetItem).isNull()
    }

    @Test
    fun onOriginalUserClicked_navigationDelegateIsCalled() {
        // setup
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }

        // exercise
        sut.onOriginalUserClicked(requireNotNull(sut.state.value?.tweetItem).originalUser)

        // verify
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)
        val tweetingUser = tweet.originalUser
        assertThat(navigationEvents).containsExactly(TimelineEffect.Navigate.UserInfo(tweetingUser))
    }

    @Test
    fun onBodyUserClicked_navigationDelegateIsCalled() {
        // setup
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }

        // exercise
        sut.onBodyUserClicked(requireNotNull(sut.state.value?.tweetItem).body.user)

        // verify
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)
        val tweetingUser = tweet.body.user
        assertThat(navigationEvents).containsExactly(TimelineEffect.Navigate.UserInfo(tweetingUser))
    }

    @Test
    fun onMediaItemClicked_navigationDelegateIsCalled() {
        // setup
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }
        val tweetId = tweet.body.id

        // exercise
        sut.onMediaItemClicked(tweet.body.id, 0)

        // verify
        assertThat(sut.state.value?.tweetItem).isEqualTo(tweet)
        assertThat(navigationEvents).containsExactly(TimelineEffect.Navigate.MediaViewer(tweetId))
    }
}
