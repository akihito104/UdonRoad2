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

package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.postEvents
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.freshdigitable.udonroad2.timeline.TimelineEvent.TweetItemSelection
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
class TimelineViewModelSourceTest {
    @get:Rule
    val rule = TimelineViewStatesTestRule()

    @Test
    fun selectedItemId_dispatchMediaItemClickedEvent_then_selectedItemIdHasValue(): Unit =
        with(rule) {
            // setup
            dispatchEvents(TimelineEvent.Setup())

            // exercise
            sut.onMediaItemClicked(TweetId(1000), index = 0, id = TweetId(1000))

            // verify
            assertThat(selectedItems.last()?.originalId).isEqualTo(TweetId(1000L))
            assertThat(navEvents[0]).isInstanceOf(TimelineEffect.Navigate.MediaViewer::class.java)
        }

    @Test
    fun selectedItemId_dispatchToggleEvent_then_hasItem(): Unit = with(rule) {
        // exercise
        dispatchEvents(
            TweetItemSelection.Toggle(SelectedItemId(owner, TweetId(200)))
        )

        // verify
        assertThat(selectedItems.last()?.originalId).isEqualTo(TweetId(200L))
    }

    @Test
    fun selectedItemId_dispatchUnselectEvent_then_hasNoItem(): Unit = with(rule) {
        // exercise
        dispatchEvents(
            TweetItemSelection.Toggle(SelectedItemId(owner, TweetId(200))),
            TweetItemSelection.Unselected(owner)
        )

        // verify
        assertThat(selectedItems.last()).isNull()
    }

    @Test
    fun updateTweet_dispatchLikeIsSuccess_then_likeDispatched(): Unit = with(rule) {
        // setup
        tweetRepositoryMock.setupPostLikeForSuccess(TweetId(200))
        sut.selectBodyItem.dispatch(TweetListItem.createMock(TweetId(200)))

        // exercise
        dispatchEvents(
            SelectedItemShortcut.Like(TweetId(200))
        )

        // verify
        assertThat(selectedItems.last()?.originalId).isEqualTo(TweetId(200L))
        assertThat(messageEvents.last().messageRes).isEqualTo(R.string.msg_fav_create_success)
    }

    @Test
    fun updateTweet_dispatchLikeIsFailure_then_likeDispatchedWithError(): Unit = with(rule) {
        // setup
        tweetRepositoryMock.setupPostLikeForFailure(
            TweetId(200), AppTwitterException.ErrorType.ALREADY_FAVORITED
        )
        sut.selectBodyItem.dispatch(TweetListItem.createMock(TweetId(200)))

        // exercise
        dispatchEvents(
            SelectedItemShortcut.Like(TweetId(200))
        )

        // verify
        assertThat(selectedItems.last()?.originalId).isEqualTo(TweetId(200L))
        assertThat(messageEvents.last().messageRes).isEqualTo(R.string.msg_already_fav)
    }

    @Test
    fun updateTweet_dispatchRetweetEvent_then_retweetDispatched(): Unit = with(rule) {
        // setup
        tweetRepositoryMock.setupPostRetweetForSuccess(TweetId(200))
        sut.selectBodyItem.dispatch(TweetListItem.createMock(TweetId(200)))

        // exercise
        dispatchEvents(
            SelectedItemShortcut.Retweet(TweetId(200))
        )

        // verify
        assertThat(selectedItems.last()?.originalId).isEqualTo(TweetId(200L))
        assertThat(messageEvents.last().messageRes).isEqualTo(R.string.msg_rt_create_success)
    }

    @Test
    fun updateTweet_dispatchRetweetIsFailure_then_retweetResultIsDispatchedWithException(): Unit =
        with(rule) {
            // setup
            tweetRepositoryMock.setupPostRetweetForFailure(
                TweetId(200), AppTwitterException.ErrorType.ALREADY_RETWEETED
            )
            sut.selectBodyItem.dispatch(TweetListItem.createMock(TweetId(200)))

            // exercise
            dispatchEvents(
                SelectedItemShortcut.Retweet(TweetId(200))
            )

            // verify
            assertThat(selectedItems.last()?.originalId).isEqualTo(TweetId(200L))
            assertThat(messageEvents.last().messageRes).isEqualTo(R.string.msg_already_rt)
        }
}

class TimelineViewStatesTestRule(
    isStateCollected: Boolean = true,
) : TestWatcher() {
    @ExperimentalCoroutinesApi
    internal val coroutineTestRule = CoroutineTestRule()
    internal val eventCollector =
        if (isStateCollected) ObserverEventCollector(coroutineTestRule) else null
    internal val actionsRule: TimelineActionsTestRule =
        TimelineActionsTestRule(coroutineTestRule, false)
    val tweetRepositoryMock = TweetRepositoryRule()
    val owner: ListOwner<QueryType.TweetQueryType> =
        actionsRule.owner as ListOwner<QueryType.TweetQueryType>
    private val listRepositoryRule =
        MockVerified.create<ListRepository<QueryType.TweetQueryType, Any>>().apply {
            coSetupResponseWithVerify({ mock.clear(any()) }, Unit)
        }
    private val listProviderRule =
        MockVerified.create<PagedListProvider<QueryType.TweetQueryType, Any>>().apply {
            setupResponseWithVerify(
                { mock.getList(owner.query, owner.id) },
                emptyFlow()
            )
        }
    private val appSettingRepository = MockVerified.create<AppSettingRepository>().apply {
        setupResponseWithVerify({ mock.isPossiblySensitiveHidden }, flowOf(true))
    }
    internal val sut: TimelineViewModelSource by lazy {
        TimelineViewModelSource(
            owner,
            actionsRule.sut,
            SelectedItemRepository(),
            tweetRepositoryMock.mock,
            ListOwnerGenerator.create(AtomicInteger(1)),
            ListItemLoadableViewStateImpl(
                owner as ListOwner<QueryType>,
                actionsRule.sut,
                listRepositoryRule.mock as ListRepository<QueryType, Any>,
                listProviderRule.mock as PagedListProvider<QueryType, Any>,
            ),
            TweetMediaViewModelSource.create(
                actionsRule.sut,
                appSettingRepository.mock,
            )
        )
    }
    val navEvents: List<AppEffect>
        get() = requireNotNull(eventCollector).nonNullEventsOf(sut.effect)
    val messageEvents: List<FeedbackMessage>
        get() = navEvents.filterIsInstance<FeedbackMessage>()
    val selectedItems: List<SelectedItemId?>
        get() = requireNotNull(eventCollector).nonNullEventsOf(sut.state).map { it.selectedItemId }

    fun setupPrependListResponse(res: List<TweetEntity> = emptyList()) = with(listRepositoryRule) {
        coSetupResponseWithVerify(
            target = { mock.prependList(owner.query, owner.id) },
            res
        )
    }

    fun dispatchEvents(vararg event: AppEvent) {
        actionsRule.dispatcher.postEvents(*event)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        eventCollector?.setupForActivate {
            with(sut) {
                addAll(state, mediaState, effect)
            }
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        val chain = eventCollector?.let { RuleChain.outerRule(it) } ?: RuleChain.emptyRuleChain()
        return chain.around(actionsRule)
            .around(listRepositoryRule)
            .around(listProviderRule)
            .around(tweetRepositoryMock)
            .around(appSettingRepository)
            .around(RxExceptionHandler())
            .apply(super.apply(base, description), description)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        coroutineTestRule.runBlockingTest {
            sut.clear()
        }
    }
}
