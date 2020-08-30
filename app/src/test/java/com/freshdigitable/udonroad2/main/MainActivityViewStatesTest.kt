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
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test.MockVerified
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.reactivex.disposables.CompositeDisposable
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

    // TODO: move to each modules
//    @Test
//    fun containerState_dispatchSetupEvent_then_showOauth(): Unit = with(rule) {
//        // setup
//        oauthTokenRepositoryMock.setupCurrentUserId(null)
//
//        // exercise
//        dispatchEvents(TimelineEvent.Setup())
//
//        // verify
//        assertThat(sut.selectedItemId.value).isEqualTo(null)
//    }
//
//    @Test
//    fun fabVisible_dispatchToggleSelectedItemEvent_then_fabVisibleIsTrue(): Unit = with(rule) {
//        // setup
//        oauthTokenRepositoryMock.setupCurrentUserId(10000)
//        dispatchEvents(TimelineEvent.Setup())
//        selectedItemRepository.put(
//            SelectedItemId(
//                ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
//            )
//        )
//
//        // verify
//        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
//        assertThat(sut.isFabVisible.value).isTrue()
//    }
//
//    @Test
//    fun updateTweet_dispatchLikeIsSuccess_then_likeDispatched(): Unit = with(rule) {
//        // setup
//        oauthTokenRepositoryMock.setupCurrentUserId(10000)
//        tweetRepositoryMock.setupPostLikeForSuccess(TweetId(200))
//        dispatchEvents(TimelineEvent.Setup())
//        selectedItemRepository.put(
//            SelectedItemId(ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200))
//        )
//
//        // exercise
//        dispatchEvents(
//            TimelineEvent.SelectedItemShortcut.Like(TweetId(200))
//        )
//
//        // verify
//        assertThat(sut.isFabVisible.value).isTrue()
//        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
//        updateTweetObserver.assertValueCount(1)
//        updateTweetObserver.assertValueAt(0) {
//            it.messageRes == R.string.msg_fav_create_success
//        }
//    }
//
//    @Test
//    fun updateTweet_dispatchLikeIsFailure_then_likeDispatchedWithError(): Unit = with(rule) {
//        // setup
//        oauthTokenRepositoryMock.setupCurrentUserId(10000)
//        tweetRepositoryMock.setupPostLikeForFailure(
//            TweetId(200), AppTwitterException.ErrorType.ALREADY_FAVORITED
//        )
//        dispatchEvents(TimelineEvent.Setup())
//        selectedItemRepository.put(
//            SelectedItemId(ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200))
//        )
//
//        // exercise
//        dispatchEvents(
//            TimelineEvent.SelectedItemShortcut.Like(TweetId(200))
//        )
//
//        // verify
//        assertThat(sut.isFabVisible.value).isTrue()
//        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
//        updateTweetObserver.assertValueCount(1)
//        updateTweetObserver.assertValueAt(0) {
//            it.messageRes == R.string.msg_already_fav
//        }
//    }
//
//    @Test
//    fun updateTweet_dispatchRetweetEvent_then_retweetDispatched(): Unit = with(rule) {
//        // setup
//        oauthTokenRepositoryMock.setupCurrentUserId(10000)
//        tweetRepositoryMock.setupPostRetweetForSuccess(TweetId(200))
//        dispatchEvents(TimelineEvent.Setup())
//        selectedItemRepository.put(
//            SelectedItemId(ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200))
//        )
//
//        // exercise
//        dispatchEvents(
//            TimelineEvent.SelectedItemShortcut.Retweet(TweetId(200))
//        )
//
//        // verify
//        assertThat(sut.isFabVisible.value).isTrue()
//        assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
//        updateTweetObserver.assertValueCount(1)
//        updateTweetObserver.assertValueAt(0) {
//            it.messageRes == R.string.msg_rt_create_success
//        }
//    }
//
//    @Test
//    fun updateTweet_dispatchRetweetIsFailure_then_retweetResultIsDispatchedWithException(): Unit =
//        with(rule) {
//            // setup
//            oauthTokenRepositoryMock.setupCurrentUserId(10000)
//            tweetRepositoryMock.setupPostRetweetForFailure(
//                TweetId(200), AppTwitterException.ErrorType.ALREADY_RETWEETED
//            )
//            dispatchEvents(TimelineEvent.Setup())
//            selectedItemRepository.put(
//                SelectedItemId(ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200))
//            )
//
//            // exercise
//            dispatchEvents(
//                TimelineEvent.SelectedItemShortcut.Retweet(TweetId(200))
//            )
//
//            // verify
//            assertThat(sut.isFabVisible.value).isTrue()
//            assertThat(sut.selectedItemId.value?.originalId).isEqualTo(TweetId(200L))
//            updateTweetObserver.assertValueCount(1)
//            updateTweetObserver.assertValueAt(0) {
//                it.messageRes == R.string.msg_already_rt
//            }
//        }

    @Test
    fun setupEventDispatched_then_dispatchNavigateCalled(): Unit = with(rule) {
        oauthTokenRepositoryMock.setupCurrentUserId(null)
        every { navDelegate.dispatchNavHostNavigate(any()) } just runs

        dispatchEvents(TimelineEvent.Setup())

        verify {
            navDelegate.dispatchNavHostNavigate(match {
                it is TimelineEvent.Navigate.Timeline && it.owner.query == QueryType.Oauth
            })
        }
    }

    @Test
    fun backEventDispatched_then_dispatchBackCalled(): Unit = with(rule) {
        every { navDelegate.dispatchBack() } just runs

        dispatchEvents(CommonEvent.Back(null))

        verify { navDelegate.dispatchBack() }
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

class TweetRepositoryRule(
    val tweetRepository: TweetRepository = mockk(),
    private val mockVerified: MockVerified = MockVerified(listOf(tweetRepository))
) : TestRule by mockVerified {

    fun setupPostLikeForSuccess(tweetId: TweetId, liked: TweetEntity = mockk()) {
        setupPostLike(tweetId, Result.success(liked))
    }

    fun setupPostLikeForFailure(tweetId: TweetId, exceptionType: AppTwitterException.ErrorType) {
        val exception = createException(exceptionType)
        setupPostLike(tweetId, Result.failure(exception))
    }

    private fun setupPostLike(tweetId: TweetId, result: Result<TweetEntity>) {
        every { tweetRepository.postLike(tweetId) } returns AppAction.just(result)
        mockVerified.expected { verify { tweetRepository.postLike(tweetId) } }
    }

    fun setupPostRetweetForSuccess(tweetId: TweetId, retweeted: TweetEntity = mockk()) {
        setupPostRetweet(tweetId, Result.success(retweeted))
    }

    fun setupPostRetweetForFailure(tweetId: TweetId, exceptionType: AppTwitterException.ErrorType) {
        val exception = createException(exceptionType)
        setupPostRetweet(tweetId, Result.failure(exception))
    }

    private fun setupPostRetweet(tweetId: TweetId, result: Result<TweetEntity>) {
        every { tweetRepository.postRetweet(tweetId) } returns AppAction.just(result)
        mockVerified.expected { verify { tweetRepository.postRetweet(tweetId) } }
    }

    private fun createException(exceptionType: AppTwitterException.ErrorType): AppTwitterException {
        return mockk<AppTwitterException>().apply {
            every { statusCode } returns exceptionType.statusCode
            every { errorCode } returns exceptionType.errorCode
            every { errorType } returns exceptionType
        }
    }
}

class MainActivityNavigationDelegateRule(
    val mock: MainActivityNavigationDelegate = mockk(relaxed = true),
    private val mockVerified: MockVerified = MockVerified(listOf(mock))
) : TestRule by mockVerified {
    private val containerStateSource = MutableLiveData<MainNavHostState>()

    init {
        every { mock.containerState } returns containerStateSource
        mockVerified.expected { verify { mock.containerState } }
        every { mock.disposables } returns CompositeDisposable()
        mockVerified.expected { verify { mock.disposables } }
    }

    fun setupContainerState(state: MainNavHostState) {
        containerStateSource.value = state
    }
}

class MainActivityStateModelTestRule : TestWatcher() {
    private val actionsTestRule = MainActivityActionsTestRule()
    val dispatcher = actionsTestRule.dispatcher
    val oauthTokenRepositoryMock = actionsTestRule.oauthTokenRepositoryMock
    private val tweetRepositoryMock = TweetRepositoryRule()
    val selectedItemRepository = SelectedItemRepository()
    val navDelegateRule = MainActivityNavigationDelegateRule()
    val navDelegate: MainActivityNavigationDelegate = navDelegateRule.mock

    val sut = MainActivityViewStates(
        actionsTestRule.sut,
        selectedItemRepository,
        navDelegateRule.mock
    )

    fun dispatchEvents(vararg events: AppEvent) {
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
            .around(actionsTestRule)
            .around(tweetRepositoryMock)
            .around(navDelegateRule)
            .apply(super.apply(base, description), description)
    }
}
