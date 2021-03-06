/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.model.CustomTimelineId
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.ListItemLoadableActions
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewStateImpl
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class CustomTimelineListViewModelTest {
    private val owner: ListOwner<QueryType.CustomTimelineListQueryType> =
        ListOwner(ListId(2), QueryType.CustomTimelineListQueryType.Ownership())

    @get:Rule
    val rule = ListItemLoadableViewStateRule(owner)

    private val sut: CustomTimelineListViewModel by lazy {
        val userIconClickedAction = UserIconClickedAction(rule.eventDispatcher)
        val viewState = CustomTimelineListItemLoadableViewState(
            CustomTimelineListActions(rule.eventDispatcher),
            rule.viewModelSource,
            ListOwnerGenerator.create(),
        )
        CustomTimelineListViewModel(viewState, UserIconViewModelSource(userIconClickedAction))
    }

    @Test
    fun onUserIconClicked() {
        // setup
        val user = mockk<TweetUserItem>().also {
            every { it.id } returns UserId(3000)
        }
        val executor = AppExecutor(dispatcher = rule.coroutineTestRule.coroutineContextProvider)
        val navEventActual = sut.navigationEvent.testCollect(executor)

        // exercise
        sut.onUserIconClicked(user)

        // verify
        assertFor<TimelineEvent.Navigate.UserInfo>(navEventActual.last()) {
            assertThat(it.tweetUserItem.id).isEqualTo(user.id)
        }
    }

    @Test
    fun onBodyItemClicked() {
        // setup
        val item = mockk<CustomTimelineItem>().also {
            every { it.id } returns CustomTimelineId(3000)
            every { it.name } returns "custom timeline"
        }
        val executor = AppExecutor(dispatcher = rule.coroutineTestRule.coroutineContextProvider)
        val navEventActual = sut.navigationEvent.testCollect(executor)

        // exercise
        sut.onBodyItemClicked(item)

        // verify
        assertFor<TimelineEvent.Navigate.Timeline>(navEventActual.last()) { actualEvent ->
            assertFor<ListOwner<QueryType.TweetQueryType.CustomTimeline>>(actualEvent.owner) {
                assertThat(it.query.id).isEqualTo(item.id)
            }
        }
    }
}

class ListItemLoadableViewStateRule(
    private val owner: ListOwner<*>,
    val eventDispatcher: EventDispatcher = EventDispatcher(),
) : TestRule {
    val coroutineTestRule = CoroutineTestRule()
    private val listRepository = MockVerified.create<ListRepository<QueryType, Any>>()
    private val pagedListProvider = MockVerified.create<PagedListProvider<QueryType, Any>>().apply {
        setupResponseWithVerify({ mock.getList(owner.query, owner.id) }, emptyFlow())
    }
    val viewModelSource: ListItemLoadableViewModelSource by lazy {
        ListItemLoadableViewStateImpl(
            owner as ListOwner<QueryType>,
            ListItemLoadableActions(owner, eventDispatcher),
            listRepository.mock,
            pagedListProvider.mock,
        )
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(coroutineTestRule)
            .around(listRepository)
            .around(pagedListProvider)
            .apply(base, description)
    }
}

inline fun <reified T> assertFor(actual: Any, body: (T) -> Unit) {
    assertThat(actual).isInstanceOf(T::class.java)
    body(actual as T)
}
