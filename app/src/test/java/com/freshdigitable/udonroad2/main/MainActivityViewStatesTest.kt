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
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.test_common.jvm.AppSettingRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainActivityViewStatesTest {
    @get:Rule
    internal val rule = MainActivityStateModelTestRule()

    @Test
    fun updateContainer_dispatchSetupEvent_then_flowInitOauthEvent(): Unit = with(rule) {
        // setup
        appSettingRepositoryRule.setupCurrentUserId(null)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        assertThatNavigationEventOfTimeline(0) {
            assertThat(it.owner.query).isEqualTo(QueryType.Oauth)
        }
    }

    @Test
    fun updateContainer_dispatchSetupEvent_then_TimelineQueryIsFlowing(): Unit = with(rule) {
        // setup
        appSettingRepositoryRule.setupCurrentUserId(authenticatedUserId.value)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        assertThatNavigationEventOfTimeline(0) {
            assertThat(it.owner.query).isInstanceOf(QueryType.TweetQueryType.Timeline::class.java)
        }
    }

    @Test
    fun setupEventDispatched_then_dispatchNavigateCalled(): Unit = with(rule) {
        // setup
        appSettingRepositoryRule.setupCurrentUserId(null)

        // exercise
        dispatchEvents(TimelineEvent.Setup())

        // verify
        assertThatNavigationEventOfTimeline(0) {
            assertThat(it.owner.query).isEqualTo(QueryType.Oauth)
        }
    }
}

internal class MainActivityNavigationDelegateRule(
    private val _mock: MockVerified<MainActivityNavigationDelegate> = MockVerified.create(true),
    val state: MainActivityNavState = MainActivityNavState()
) : TestRule by _mock {

    fun setupContainerState(state: MainNavHostState) {
        this.state.setContainerState(state)
    }

    fun setIsInTopLevelDestination(isInTop: Boolean) {
        this.state.setIsInTopLevelDest(isInTop)
    }
}

internal class MainActivityStateModelTestRule(
    val dispatcher: EventDispatcher = EventDispatcher(),
    val appSettingRepositoryRule: AppSettingRepositoryRule = AppSettingRepositoryRule(),
    val oauthTokenRepository: OAuthTokenRepositoryRule = OAuthTokenRepositoryRule(
        appSettingRepositoryRule
    ),
    val coroutineRule: CoroutineTestRule = CoroutineTestRule(),
    private val isStateCollected: Boolean = true,
) : TestWatcher() {
    val selectedItemRepository = SelectedItemRepository()
    val navDelegateRule = MainActivityNavigationDelegateRule()

    val isExpandedSource = MutableStateFlow(false)
    private val tweetInputSharedState = MockVerified.create<TweetInputSharedState>().apply {
        every { mock.isExpanded } returns isExpandedSource
    }
    val authenticatedUserId = UserId(10000)
    val coroutineScope =
        CoroutineScope(coroutineRule.coroutineContextProvider.mainContext + SupervisorJob())
    val sut: MainViewModelSource by lazy {
        MainViewModelSource(
            MainActivityActions(dispatcher),
            selectedItemRepository,
            oauthTokenRepository.appSettingMock,
            tweetInputSharedState.mock,
            ListOwnerGenerator.create(),
            navDelegateRule.state,
        )
    }
    private lateinit var navigationEventActual: List<NavigationEvent>

    fun dispatchEvents(vararg events: AppEvent) {
        dispatcher.postEvents(*events)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        if (isStateCollected) {
            navigationEventActual = sut.initContainer.testCollect(coroutineScope)
            sut.states.testCollect(coroutineScope)
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(coroutineRule)
            .around(navDelegateRule)
            .around(oauthTokenRepository)
            .around(RxExceptionHandler())
            .apply(super.apply(base, description), description)
    }

    fun assertThatNavigationEventOfTimeline(
        index: Int,
        matcher: (TimelineEvent.Navigate.Timeline) -> Unit
    ) {
        navigationEventActual.assertThatNavigationEvent(index, matcher)
    }
}

inline fun <reified T : NavigationEvent> List<NavigationEvent>.assertThatNavigationEvent(
    index: Int,
    matcher: (T) -> Unit
) {
    with(this[index]) {
        assertThat(this).isInstanceOf(T::class.java)
        matcher(this as T)
    }
}
