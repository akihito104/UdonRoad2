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
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
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
        oauthTokenRepositoryMock.setupCurrentUserId(null)

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
        oauthTokenRepositoryMock.setupCurrentUserId(10000)

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
        oauthTokenRepositoryMock.setupCurrentUserId(null)

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

internal class MainActivityStateModelTestRule : TestWatcher() {
    val dispatcher = EventDispatcher()
    val oauthTokenRepositoryMock = OAuthTokenRepositoryRule()
    val selectedItemRepository = SelectedItemRepository()
    val navDelegateRule = MainActivityNavigationDelegateRule()
    private val coroutineRule = CoroutineTestRule()

    val isExpandedSource = MutableLiveData<Boolean>()
    private val tweetInputSharedState = MockVerified.create<TweetInputSharedState>().apply {
        every { mock.isExpanded } returns isExpandedSource
    }

    val sut = MainActivityViewStates(
        MainActivityActions(dispatcher),
        selectedItemRepository,
        oauthTokenRepositoryMock.appSettingMock,
        tweetInputSharedState.mock,
        ListOwnerGenerator.create(),
        navDelegateRule.state,
    )
    lateinit var navigationEventActual: List<NavigationEvent>

    fun dispatchEvents(vararg events: AppEvent) {
        dispatcher.postEvents(*events)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        listOf(
            sut.isFabVisible, sut.appBarTitle, sut.navIconType
        ).forEach { it.observeForever {} }
        navigationEventActual = sut.initContainer.testCollect(
            AppExecutor(dispatcher = coroutineRule.coroutineContextProvider)
        )
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(coroutineRule)
            .around(navDelegateRule)
            .around(RxExceptionHandler())
            .apply(super.apply(base, description), description)
    }

    fun assertThatNavigationEventOfTimeline(
        index: Int,
        matcher: (TimelineEvent.Navigate.Timeline) -> Unit
    ) {
        with(navigationEventActual[index]) {
            assertThat(this).isInstanceOf(TimelineEvent.Navigate.Timeline::class.java)
            matcher(this as TimelineEvent.Navigate.Timeline)
        }
    }
}
