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

import com.freshdigitable.fabshortcut.FlingFAB
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.input.TweetInputSharedState
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.test_common.jvm.AppSettingRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.assertLatestNavigationEvent
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainViewModelSourceTest {
    @get:Rule
    internal val rule = MainViewModelSourceTestRule()

    @Test
    fun updateContainer_dispatchSetupEvent_then_flowInitOauthEvent(): Unit = with(rule) {
        // setup
        appSettingRepositoryRule.setupCurrentUserId(null)

        // exercise
        dispatcher.postEvent(TimelineEvent.Setup())

        // verify
        eventCollector.assertLatestNavigationEvent<TimelineEffect.Navigate.Timeline>(
            sut.initContainer
        ) {
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
        eventCollector.assertLatestNavigationEvent<TimelineEffect.Navigate.Timeline>(
            sut.initContainer
        ) {
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
        eventCollector.assertLatestNavigationEvent<TimelineEffect.Navigate.Timeline>(
            sut.initContainer
        ) {
            assertThat(it.owner.query).isEqualTo(QueryType.Oauth)
        }
    }

    @Test
    fun containerUpdatedToDetail_then_shortcutStateIsToolbar(): Unit = with(rule) {
        // setup
        appSettingRepositoryRule.setupCurrentUserId(authenticatedUserId.value)
        tweetRepositoryRule.setupShowTweet(TweetId(4000),
            flowOf(DetailTweetListItem.createMock(TweetId(4000))))
        dispatchEvents(TimelineEvent.Setup())

        // exercise
        navDelegateRule.setupContainerState(MainNavHostState.TweetDetail(TweetId(4000)))

        // verify
        val state = eventCollector.eventsOf(sut.states).last()
        assertThat(state?.mode).isEqualTo(FlingFAB.Mode.TOOLBAR)
        assertThat(state?.menuItemState?.isMainGroupEnabled).isTrue()
    }
}

internal class MainActivityNavigationDelegateRule(
    private val _mock: MockVerified<MainActivityNavigationDelegate> = MockVerified.create(true),
    val state: MainActivityNavState = MainActivityNavState(),
) : TestRule by _mock {

    fun setupContainerState(state: MainNavHostState) {
        this.state.setContainerState(state)
    }

    fun setIsInTopLevelDestination(isInTop: Boolean) {
        this.state.setIsInTopLevelDest(isInTop)
    }
}

internal class MainViewModelSourceTestRule(
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
    val eventCollector = ObserverEventCollector(coroutineRule)

    val isExpandedSource = MutableStateFlow(false)
    private val tweetInputSharedState = MockVerified.create<TweetInputSharedState>().apply {
        every { mock.isExpanded } returns isExpandedSource
    }
    val authenticatedUserId = UserId(10000)
    val tweetRepositoryRule = TweetRepositoryRule()
    val sut: MainViewModelSource by lazy {
        MainViewModelSource(
            MainActivityActions(dispatcher),
            selectedItemRepository,
            oauthTokenRepository.appSettingMock,
            tweetRepositoryRule.mock,
            tweetInputSharedState.mock,
            ListOwnerGenerator.create(),
            navDelegateRule.state,
        )
    }

    fun dispatchEvents(vararg events: AppEvent) {
        dispatcher.postEvents(*events)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        appSettingRepositoryRule.setupCurrentUserIdSource(authenticatedUserId.value)
        if (isStateCollected) {
            eventCollector.setupForActivate {
                addAll(sut.states, sut.initContainer)
            }
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(eventCollector)
            .around(navDelegateRule)
            .around(oauthTokenRepository)
            .around(tweetRepositoryRule)
            .around(RxExceptionHandler())
            .apply(super.apply(base, description), description)
    }
}
