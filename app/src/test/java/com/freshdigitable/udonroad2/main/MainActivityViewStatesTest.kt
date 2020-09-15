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
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import io.mockk.every
import io.mockk.mockk
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

    @Test
    fun updateContainer_dispatchSetupEvent_then_flowInitOauthEvent(): Unit = with(rule) {
        // setup
        oauthTokenRepositoryMock.setupCurrentUserId(null)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        verify {
            navDelegate.dispatchNavHostNavigate(match {
                it is TimelineEvent.Navigate.Timeline && it.owner.query is QueryType.Oauth
            })
        }
    }

    @Test
    fun updateContainer_dispatchSetupEvent_then_TimelineQueryIsFlowing(): Unit = with(rule) {
        // setup
        oauthTokenRepositoryMock.setupCurrentUserId(10000)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        verify {
            navDelegate.dispatchNavHostNavigate(match {
                it is TimelineEvent.Navigate.Timeline &&
                    it.owner.query is QueryType.TweetQueryType.Timeline
            })
        }
    }

    @Test
    fun setupEventDispatched_then_dispatchNavigateCalled(): Unit = with(rule) {
        // setup
        oauthTokenRepositoryMock.setupCurrentUserId(null)

        // exercise
        dispatchEvents(TimelineEvent.Setup())

        // verify
        verify {
            navDelegate.dispatchNavHostNavigate(match {
                it is TimelineEvent.Navigate.Timeline && it.owner.query == QueryType.Oauth
            })
        }
    }

    @Test
    fun backEventDispatched_then_dispatchBackCalled(): Unit = with(rule) {
        // exercise
        dispatchEvents(CommonEvent.Back)

        // verify
        verify { navDelegate.dispatchBack() }
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
    val dispatcher = EventDispatcher()
    val oauthTokenRepositoryMock = OAuthTokenRepositoryRule()
    val selectedItemRepository = SelectedItemRepository()
    val navDelegateRule = MainActivityNavigationDelegateRule()
    val navDelegate: MainActivityNavigationDelegate = navDelegateRule.mock

    val sut = MainActivityViewStates(
        MainActivityActions(dispatcher),
        selectedItemRepository,
        oauthTokenRepositoryMock.mock,
        ListOwnerGenerator(),
        navDelegateRule.mock
    )

    fun dispatchEvents(vararg events: AppEvent) {
        dispatcher.postEvents(*events)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        listOf(
            sut.isFabVisible,
        ).forEach { it.observeForever {} }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(navDelegateRule)
            .around(RxExceptionHandler())
            .apply(super.apply(base, description), description)
    }
}
