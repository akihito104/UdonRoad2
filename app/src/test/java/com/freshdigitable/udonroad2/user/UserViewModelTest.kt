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

package com.freshdigitable.udonroad2.user

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified2
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

class UserViewModelTest {
    @get:Rule
    val rule = UserViewModelTestRule()

    @Test
    fun initialValue(): Unit = with(rule) {
        // verify
        assertThat(sut.user.value).isNotNull()
        assertThat(sut.relationship.value).isNotNull()
        assertThat(sut.fabVisible.value).isFalse()
        assertThat(sut.titleAlpha.value).isEqualTo(0)
    }

    @Test
    fun setAppbarScrollRate(): Unit = with(rule) {
        // exercise
        sut.setAppBarScrollRate(1f)

        // verify
        assertThat(sut.titleAlpha.value).isEqualTo(1f)
    }

    @Test
    fun updateFollowingStatus(): Unit = with(rule) {
        // setup
        every { relationshipRepository.updateFollowingStatus(targetId, any()) } just runs

        // exercise
        sut.updateFollowingStatus(true)

        // verify
        verify { relationshipRepository.updateFollowingStatus(targetId, true) }
    }

    @Test
    fun updateBlockingStatus(): Unit = with(rule) {
        // setup
        every { relationshipRepository.updateBlockingStatus(targetId, any()) } just runs

        // exercise
        sut.updateBlockingStatus(true)

        // verify
        verify { relationshipRepository.updateBlockingStatus(targetId, true) }
    }

    @Test
    fun updateMutingStatus(): Unit = with(rule) {
        // setup
        every { relationshipRepository.updateMutingStatus(targetId, any()) } just runs

        // exercise
        sut.updateMutingStatus(true)

        // verify
        verify { relationshipRepository.updateMutingStatus(targetId, true) }
    }

    @Test
    fun reportForSpam(): Unit = with(rule) {
        // setup
        every { relationshipRepository.reportSpam(targetId) } just runs

        // exercise
        sut.reportForSpam()

        // verify
        verify { relationshipRepository.reportSpam(targetId) }
    }

    @Test
    fun fabVisible_dispatchedSelectedEvent_then_fabVisibleIsTrue(): Unit = with(rule) {
        // setup
        sut.setCurrentPage(0)

        // exercise
        selectedItemRepository.put(
            SelectedItemId(
                ListOwner(0, QueryType.TweetQueryType.Timeline(targetId)), TweetId(10000)
            )
        )

        // verify
        assertThat(sut.fabVisible.value).isTrue()
    }

    @Test
    fun fabVisible_WhenItemSelectedChangeCurrentPage_then_fabVisibleIsFalse(): Unit = with(rule) {
        // setup
        sut.setCurrentPage(0)
        selectedItemRepository.put(
            SelectedItemId(
                ListOwner(0, QueryType.TweetQueryType.Timeline(targetId)), TweetId(10000)
            )
        )

        // exercise
        sut.setCurrentPage(1)

        // verify
        assertThat(sut.fabVisible.value).isFalse()
    }

    @Test
    fun fabVisible_WhenItemSelectedReturnToPage_then_fabVisibleIsTrue(): Unit = with(rule) {
        // setup
        sut.setCurrentPage(0)
        selectedItemRepository.put(
            SelectedItemId(
                ListOwner(0, QueryType.TweetQueryType.Timeline(targetId)), TweetId(10000)
            )
        )
        sut.setCurrentPage(1)

        // exercise
        sut.setCurrentPage(0)

        // verify
        assertThat(sut.fabVisible.value).isTrue()
    }
}

class UserViewModelTestRule : TestWatcher() {
    val targetId = UserId(1000)
    private val targetUser: TweetingUser = mockk<TweetingUser>().apply {
        every { id } returns targetId
        every { screenName } returns "user1"
    }
    private val userRepository = MockVerified2.create<UserRepository>()
    private val _relationshipRepository = MockVerified2.create<RelationshipRepository>()
    val relationshipRepository: RelationshipRepository = _relationshipRepository.mock
    val selectedItemRepository = SelectedItemRepository()

    val sut: UserViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        val viewStates = UserActivityViewStates(
            targetUser,
            UserActivityActions(eventDispatcher),
            userRepository.mock,
            relationshipRepository,
            selectedItemRepository,
            ListOwnerGenerator(),
            mockk(relaxed = true)
        )
        UserViewModel(targetUser, eventDispatcher, viewStates)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        setupUser(targetId)
        setupRelation(targetId)

        sut.setCurrentPage(0)
        sut.setAppBarScrollRate(0f)
        listOf(
            sut.user,
            sut.relationship,
            sut.fabVisible,
            sut.titleAlpha
        ).forEach { it.observeForever {} }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(userRepository)
            .around(_relationshipRepository)
            .apply(super.apply(base, description), description)
    }

    private fun setupUser(targetUser: UserId) {
        val response = mockk<User>().apply {
            every { id } returns targetUser
        }
        with(userRepository) {
            every { mock.getUser(targetUser) } returns MutableLiveData(response)
            expected { verify { mock.getUser(targetUser) } }
        }
    }

    private fun setupRelation(targetUser: UserId, response: Relationship = mockk()) {
        with(_relationshipRepository) {
            every { mock.findRelationship(targetUser) } returns MutableLiveData(response)
            expected { verify { mock.findRelationship(targetUser) } }
        }
    }
}
