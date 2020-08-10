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
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test.MockVerified
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainActivityViewStatesTest {
    @get:Rule
    val rule = MainActivityStateModelTestRule()

    @Test
    fun containerState_dispatchSetupEvent_then_showOauth(): Unit = with(rule) {
        // setup
        setupCurrentUserId(null)

        // exercise
        dispatchEvents(TimelineEvent.Setup())

        // verify
        assertThat(sut.selectedItemId.value).isEqualTo(null)
    }

    @Test
    fun fabVisible_dispatchToggleSelectedItemEvent_then_fabVisibleIsTrue(): Unit = with(rule) {
        // setup
        setupCurrentUserId(10000)

        // exercise
        dispatchEvents(
            TimelineEvent.Setup(),
            TimelineEvent.TweetItemSelection.Toggle(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
                )
            )
        )

        // verify
        assertThat(sut.isFabVisible.value).isTrue()
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
    }

    @Test
    fun selectedItemId_dispatchMediaItemClickedEvent_then_selectedItemIdHasValue(): Unit =
        with(rule) {
            // setup
            setupCurrentUserId(10000)

            // exercise
            dispatchEvents(
                TimelineEvent.Setup(),
                TimelineEvent.MediaItemClicked(
                    TweetId(1000),
                    0,
                    SelectedItemId(ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(1000))
                )
            )

            // verify
            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(1000L))
        }

    @Test
    fun updateTweet_dispatchLikeIsSuccess_then_likeDispatched(): Unit = with(rule) {
        // setup
        setupCurrentUserId(10000)
        val liked: TweetEntity = mockk()
        every { tweetRepository.postLike(TweetId(200)) } returns AppAction.just(Result.success(liked))
        val updateTweetObserver = sut.updateTweet.test()

        // exercise
        dispatchEvents(
            TimelineEvent.Setup(),
            TimelineEvent.TweetItemSelection.Selected(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
                )
            ),
            TimelineEvent.SelectedItemShortcut.Like(TweetId(200))
        )

        // verify
        assertThat(sut.isFabVisible.value).isTrue()
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
        updateTweetObserver.assertValueCount(1)
        updateTweetObserver.assertValueAt(0) { it.value == liked }
    }

    @Test
    fun updateTweet_dispatchLikeIsFailure_then_likeDispatchedWithError(): Unit = with(rule) {
        // setup
        setupCurrentUserId(10000)
        every { tweetRepository.postLike(TweetId(200)) } returns AppAction.just(
            Result.failure(
                mockk<AppTwitterException>().apply {
                    every { statusCode } returns 403
                    every { errorCode } returns 139
                }
            )
        )
        val updateTweetObserver = sut.updateTweet.test()

        // exercise
        dispatchEvents(
            TimelineEvent.Setup(),
            TimelineEvent.TweetItemSelection.Selected(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
                )
            ),
            TimelineEvent.SelectedItemShortcut.Like(TweetId(200))
        )

        // verify
        assertThat(sut.isFabVisible.value).isTrue()
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
        updateTweetObserver.assertValueCount(1)
        updateTweetObserver.assertValueAt(0) {
            it.event is TimelineEvent.SelectedItemShortcut.Like && it.value == null
        }
    }

    @Test
    fun updateTweet_dispatchRetweetEvent_then_retweetDispatched(): Unit = with(rule) {
        // setup
        setupCurrentUserId(10000)
        val retweeted: TweetEntity = mockk()
        every { tweetRepository.postRetweet(TweetId(200)) } returns AppAction.just(
            Result.success(
                retweeted
            )
        )
        val updateTweetObserver = sut.updateTweet.test()

        // exercise
        dispatchEvents(
            TimelineEvent.Setup(),
            TimelineEvent.TweetItemSelection.Selected(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
                )
            ),
            TimelineEvent.SelectedItemShortcut.Retweet(TweetId(200))
        )

        // verify
        assertThat(sut.isFabVisible.value).isTrue()
        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
        updateTweetObserver.assertValueCount(1)
        updateTweetObserver.assertValueAt(0) {
            it.event is TimelineEvent.SelectedItemShortcut.Retweet && it.value == retweeted
        }
    }

    @Test
    fun updateTweet_dispatchRetweetIsFailure_then_retweetResultIsDispatchedWithException(): Unit =
        with(rule) {
            // setup
            setupCurrentUserId(10000)
            every { tweetRepository.postRetweet(TweetId(200)) } returns AppAction.just(
                Result.failure(
                    mockk<AppTwitterException>().apply {
                        every { statusCode } returns 403
                        every { errorCode } returns 327
                    }
                )
            )
            val updateTweetObserver = sut.updateTweet.test()

            // exercise
            dispatchEvents(
                TimelineEvent.Setup(),
                TimelineEvent.TweetItemSelection.Selected(
                    SelectedItemId(
                        ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
                    )
                ),
                TimelineEvent.SelectedItemShortcut.Retweet(TweetId(200))
            )

            // verify
            assertThat(sut.isFabVisible.value).isTrue()
            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
            updateTweetObserver.assertValueCount(1)
            updateTweetObserver.assertValueAt(0) {
                it.event is TimelineEvent.SelectedItemShortcut.Retweet && it.value == null
            }
        }
}

class OAuthTokenRepositoryRule(
    val tokenRepository: OAuthTokenRepository = mockk(),
    private val mockVerified: MockVerified = MockVerified(listOf(tokenRepository))
) : TestRule by mockVerified {
    fun setupCurrentUserId(userId: Long?) {
        val id = UserId.create(userId)
        every { tokenRepository.getCurrentUserId() } returns id
        mockVerified.expected { verify { tokenRepository.getCurrentUserId() } }
        if (id != null) {
            every { tokenRepository.login(id) } just runs
            mockVerified.expected { verify { tokenRepository.login(id) } }
        }
    }
}

class MainActivityStateModelTestRule : TestWatcher() {
    private val dispatcher = EventDispatcher()
    private val oauthTokenRepositoryMock = OAuthTokenRepositoryRule()
    val tweetRepository: TweetRepository = mockk()
    val sut = MainActivityViewStates(
        MainActivityActions(dispatcher, oauthTokenRepositoryMock.tokenRepository),
        SelectedItemRepository(),
        tweetRepository
    )

    fun setupCurrentUserId(userId: Long?) {
        oauthTokenRepositoryMock.setupCurrentUserId(userId)
    }

    fun dispatchEvents(vararg events: NavigationEvent) {
        dispatcher.postEvents(*events)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        listOf(
            sut.isFabVisible,
            sut.selectedItemId
        ).forEach { it.observeForever {} }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(oauthTokenRepositoryMock)
            .apply(super.apply(base, description), description)
    }
}

fun EventDispatcher.postEvents(vararg events: NavigationEvent) {
    events.forEach(this::postEvent)
}
