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
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.CommonEvent
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified
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
            .around(navDelegateRule)
            .apply(super.apply(base, description), description)
    }
}
