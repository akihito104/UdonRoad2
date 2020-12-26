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
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
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
    private val oauthRepositoryRule = OAuthTokenRepositoryRule()
    private val coroutineRule = CoroutineTestRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(exceptions)
        .around(InstantTaskExecutorRule())
        .around(coroutineRule)
        .around(tweetRepositoryRule)
        .around(oauthRepositoryRule)

    private val tweet = mockk<TweetListItem>().apply {
        every { originalId } returns TweetId(1000)
        every { originalUser } returns mockk<TweetUserItem>().apply {
            every { id } returns UserId(3000)
        }
        every { body } returns mockk<TweetElement>().apply {
            every { id } returns TweetId(1001)
            every { user } returns mockk<TweetUserItem>().apply {
                every { id } returns UserId(3001)
            }
            every { isRetweeted } returns false
            every { isFavorited } returns false
        }
    }
    private val executor = AppExecutor(dispatcher = coroutineRule.coroutineContextProvider)

    private val sut: TweetDetailViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        val actions = TweetDetailActions(eventDispatcher)
        TweetDetailViewModel(
            eventDispatcher,
            TweetDetailViewStates(
                tweet.originalId,
                actions,
                tweetRepositoryRule.mock,
                oauthRepositoryRule.mock,
                executor
            ),
            executor.dispatcher.mainContext
        )
    }
    private val tweetSource: Channel<TweetListItem?> = Channel()
    private lateinit var navigationEvents: List<NavigationEvent>

    @Before
    fun setup() {
        tweetRepositoryRule.setupShowTweet(tweet.originalId, tweetSource.receiveAsFlow())
        sut.tweetItem.observeForever { }
        sut.menuItemStates.observeForever { }
        navigationEvents = sut.navigationEvent.testCollect(executor)
    }

    @Test
    fun initialState() {
        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.tweetItem.value).isNull()
        assertThat(sut.menuItemStates.value).isEqualTo(TweetDetailViewStates.MenuItemState())
    }

    @Test
    fun whenItemIsFound_then_tweetItemHasItem() {
        // setup
        oauthRepositoryRule.setupCurrentUserId(tweet.originalId.value + 10, false)

        // exercise
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }

        // verify
        assertThat(sut.tweetItem.value).isEqualTo(tweet)
        assertThat(sut.menuItemStates.value).isEqualTo(TweetDetailViewStates.MenuItemState(true))
    }

    @Test
    fun whenItemIsNotFoundInLocal_then_fetchTweetItem() {
        // setup
        tweetRepositoryRule.setupFindTweetItem(tweet.originalId, tweet)
        oauthRepositoryRule.setupCurrentUserId(tweet.originalId.value + 10, false)

        // exercise
        coroutineRule.runBlockingTest {
            tweetSource.send(null)
        }

        // verify
        assertThat(sut.tweetItem.value).isEqualTo(tweet)
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
        assertThat(sut.tweetItem.value).isNull()
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
        assertThat(sut.tweetItem.value).isNull()
    }

    @Test
    fun onOriginalUserClicked_navigationDelegateIsCalled() {
        // setup
        oauthRepositoryRule.setupCurrentUserId(tweet.originalId.value + 10, false)
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }

        // exercise
        sut.onOriginalUserClicked()

        // verify
        assertThat(sut.tweetItem.value).isEqualTo(tweet)
        val tweetingUser = tweet.originalUser
        assertThat(navigationEvents).containsExactly(TimelineEvent.Navigate.UserInfo(tweetingUser))
    }

    @Test
    fun onBodyUserClicked_navigationDelegateIsCalled() {
        // setup
        oauthRepositoryRule.setupCurrentUserId(tweet.originalId.value + 10, false)
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }

        // exercise
        sut.onBodyUserClicked()

        // verify
        assertThat(sut.tweetItem.value).isEqualTo(tweet)
        val tweetingUser = tweet.body.user
        assertThat(navigationEvents).containsExactly(TimelineEvent.Navigate.UserInfo(tweetingUser))
    }

    @Test
    fun onMediaItemClicked_navigationDelegateIsCalled() {
        // setup
        oauthRepositoryRule.setupCurrentUserId(tweet.originalId.value + 10, false)
        coroutineRule.runBlockingTest {
            tweetSource.send(tweet)
        }
        val tweetId = tweet.body.id

        // exercise
        sut.onMediaItemClicked(tweet.originalId, tweetId, tweet.body, 0)

        // verify
        assertThat(sut.tweetItem.value).isEqualTo(tweet)
        assertThat(navigationEvents).containsExactly(TimelineEvent.Navigate.MediaViewer(tweetId))
    }
}
