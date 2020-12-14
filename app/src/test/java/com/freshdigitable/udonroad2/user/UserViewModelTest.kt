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

import androidx.annotation.IdRes
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.main.menuItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MatcherScopedSuspendBlock
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.user.RelationshipMenu.BLOCK
import com.freshdigitable.udonroad2.user.RelationshipMenu.FOLLOW
import com.freshdigitable.udonroad2.user.RelationshipMenu.MUTE
import com.freshdigitable.udonroad2.user.RelationshipMenu.REPORT_SPAM
import com.freshdigitable.udonroad2.user.RelationshipMenu.RETWEET_BLOCKED
import com.freshdigitable.udonroad2.user.RelationshipMenu.RETWEET_WANTED
import com.freshdigitable.udonroad2.user.RelationshipMenu.UNBLOCK
import com.freshdigitable.udonroad2.user.RelationshipMenu.UNFOLLOW
import com.freshdigitable.udonroad2.user.RelationshipMenu.UNMUTE
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(Enclosed::class)
class UserViewModelTest {
    class Init {
        @get:Rule
        val rule = UserViewModelTestRule()

        @Test
        fun initialValue(): Unit = with(rule) {
            // verify
            assertThat(sut.user.value).isNull()
            assertThat(sut.relationship.value).isNull()
            assertThat(sut.relationshipMenuItems.value).isNull()
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
        fun user_whenNotFoundInLocal_then_getUserIsCalled(): Unit = with(rule) {
            // setup
            with(userRepositoryMock) {
                coSetupResponseWithVerify({ mock.getUser(targetId) }, user)
            }
            setupRelation(targetId, relationship())

            // exercise
            userSource.value = null

            // verify
            assertThat(sut.user.value).isNotNull()
        }

        @Test
        fun user_whenNotFoundInLocal_then_getUserIsCalledWithException(): Unit = with(rule) {
            // setup
            with(userRepositoryMock) {
                coSetupThrowWithVerify({ mock.getUser(targetId) }, IOException())
            }

            // exercise
            userSource.value = null

            // verify
            assertThat(sut.user.value).isNull()
            verify { navigationDelegate.dispatchFeedbackMessage(any()) }
        }
    }

    class WhenItemSelected {
        @get:Rule
        val rule = UserViewModelTestRule()

        @Before
        fun setup(): Unit = with(rule) {
            sut.setCurrentPage(0)
            selectedItemRepository.put(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline(targetId)), TweetId(10000)
                )
            )
        }

        @Test
        fun fabVisible_changeCurrentPage_then_fabVisibleIsFalse(): Unit = with(rule) {
            // exercise
            sut.setCurrentPage(1)

            // verify
            assertThat(sut.fabVisible.value).isFalse()
        }

        @Test
        fun fabVisible_returnToPage_then_fabVisibleIsTrue(): Unit = with(rule) {
            // setup
            sut.setCurrentPage(1)

            // exercise
            sut.setCurrentPage(0)

            // verify
            assertThat(sut.fabVisible.value).isTrue()
        }
    }

    @RunWith(Parameterized::class)
    class WhenRelationshipMenuIsSelected(private val param: Param<*>) {
        @get:Rule
        val rule = UserViewModelTestRule()

        data class Param<T>(
            val text: String,
            @IdRes val menuId: Int,
            val expectedMessage: FeedbackMessage,
            val block: UserViewModelTestRule.() -> MatcherScopedSuspendBlock<T>,
        ) {
            override fun toString(): String = "$text: expectedMessage:$expectedMessage"
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun params(): List<Param<*>> = listOf(
                Param(
                    "follow",
                    R.id.action_follow,
                    RelationshipFeedbackMessage.FOLLOW_CREATE_SUCCESS
                ) {
                    { relationshipRepository.updateFollowingStatus(targetId, true) }
                },
                Param(
                    "unfollow",
                    R.id.action_unfollow,
                    RelationshipFeedbackMessage.FOLLOW_DESTROY_SUCCESS
                ) {
                    { relationshipRepository.updateFollowingStatus(targetId, false) }
                },
                Param(
                    "mute",
                    R.id.action_mute,
                    RelationshipFeedbackMessage.MUTE_CREATE_SUCCESS
                ) {
                    { relationshipRepository.updateMutingStatus(targetId, true) }
                },
                Param(
                    "unmute",
                    R.id.action_unmute,
                    RelationshipFeedbackMessage.MUTE_DESTROY_SUCCESS
                ) {
                    { relationshipRepository.updateMutingStatus(targetId, false) }
                },
                Param(
                    "block",
                    R.id.action_block,
                    RelationshipFeedbackMessage.BLOCK_CREATE_SUCCESS
                ) {
                    { relationshipRepository.updateBlockingStatus(targetId, true) }
                },
                Param(
                    "unblock",
                    R.id.action_unblock,
                    RelationshipFeedbackMessage.BLOCK_DESTROY_SUCCESS
                ) {
                    { relationshipRepository.updateBlockingStatus(targetId, false) }
                },
                Param(
                    "block_retweet",
                    R.id.action_block_retweet,
                    RelationshipFeedbackMessage.WANT_RETWEET_DESTROY_SUCCESS
                ) {
                    { relationshipRepository.updateWantRetweetStatus(targetId, false) }
                },
                Param(
                    "want_retweet",
                    R.id.action_unblock_retweet,
                    RelationshipFeedbackMessage.WANT_RETWEET_CREATE_SUCCESS
                ) {
                    { relationshipRepository.updateWantRetweetStatus(targetId, true) }
                },
                Param(
                    "spam",
                    R.id.action_r4s,
                    RelationshipFeedbackMessage.REPORT_SPAM_SUCCESS
                ) {
                    { relationshipRepository.reportSpam(targetId) }
                },
            )
        }

        @Test
        fun testOnSuccess(): Unit = with(rule) {
            // setup
            relationshipRepositoryMock.coSetupResponseWithVerify(param.block(this), mockk())

            // exercise
            sut.onOptionsItemSelected(menuItem(param.menuId))

            // verify
            verify { navigationDelegate.dispatchFeedbackMessage(param.expectedMessage) }
        }
    }

    @RunWith(Parameterized::class)
    class WhenRelationshipUpdated(private val param: Param) {
        @get:Rule
        val rule = UserViewModelTestRule()

        data class Param(
            val givenRelationship: Relationship?,
            val menuSet: Iterable<RelationshipMenu>
        ) {
            override fun toString(): String {
                return "relationship:" + (
                    givenRelationship?.let {
                        "{following:${it.following}, blocking:${it.blocking}, " +
                            "muting:${it.muting}, wantRetweets:${it.wantRetweets}}"
                    } ?: "null"
                    ) +
                    ", menuSet:$menuSet"
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun params(): List<Param> = listOf(
                Param(
                    null,
                    setOf(REPORT_SPAM)
                ),
                Param(
                    relationship(),
                    setOf(FOLLOW, BLOCK, MUTE, REPORT_SPAM)
                ),
                Param(
                    relationship(givenBlocking = true),
                    setOf(FOLLOW, UNBLOCK, MUTE, REPORT_SPAM)
                ),
                Param(
                    relationship(givenMuting = true),
                    setOf(FOLLOW, BLOCK, UNMUTE, REPORT_SPAM)
                ),
                Param(
                    relationship(givenFollowing = true),
                    setOf(UNFOLLOW, BLOCK, MUTE, RETWEET_WANTED, REPORT_SPAM)
                ),
                Param(
                    relationship(givenFollowing = true, givenMuting = true),
                    setOf(UNFOLLOW, BLOCK, UNMUTE, RETWEET_WANTED, REPORT_SPAM)
                ),
                Param(
                    relationship(givenFollowing = true, givenWantRetweets = true),
                    setOf(UNFOLLOW, BLOCK, MUTE, RETWEET_BLOCKED, REPORT_SPAM)
                ),
                Param(
                    relationship(
                        givenFollowing = true,
                        givenMuting = true,
                        givenWantRetweets = true
                    ),
                    setOf(UNFOLLOW, BLOCK, UNMUTE, RETWEET_BLOCKED, REPORT_SPAM)
                ),
            )
        }

        @Test
        fun test(): Unit = with(rule) {
            // setup
            setupRelation(targetId, param.givenRelationship)

            // exercise
            userSource.value = user

            // verify
            assertThat(sut.relationshipMenuItems.value).containsExactlyElementsIn(param.menuSet)
        }
    }
}

@ExperimentalCoroutinesApi
class UserViewModelTestRule : TestWatcher() {
    val targetId = UserId(1000)
    private val targetUser: TweetUserItem = mockk<TweetUserItem>().apply {
        every { id } returns targetId
        every { screenName } returns "user1"
    }
    val user = mockk<UserEntity>().apply {
        every { id } returns targetId
    }
    val userRepositoryMock = MockVerified.create<UserRepository>()
    val relationshipRepositoryMock = MockVerified.create<RelationshipRepository>()
    val relationshipRepository: RelationshipRepository = relationshipRepositoryMock.mock
    val selectedItemRepository = SelectedItemRepository()
    private val coroutineRule = CoroutineTestRule()
    val navigationDelegate = mockk<UserActivityNavigationDelegate>(relaxed = true)

    val sut: UserViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        val viewStates = UserActivityViewStates(
            targetUser,
            UserActivityActions(eventDispatcher),
            userRepositoryMock.mock,
            relationshipRepository,
            selectedItemRepository,
            ListOwnerGenerator.create(),
            navigationDelegate,
            AppExecutor(dispatcher = coroutineRule.coroutineContextProvider)
        )
        UserViewModel(targetUser, eventDispatcher, viewStates)
    }

    override fun starting(description: Description?) {
        super.starting(description)
        setupUserSource(targetId)

        sut.setCurrentPage(0)
        sut.setAppBarScrollRate(0f)
        listOf(
            sut.user,
            sut.relationship,
            sut.fabVisible,
            sut.titleAlpha,
            sut.relationshipMenuItems,
        ).forEach { it.observeForever {} }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(userRepositoryMock)
            .around(relationshipRepositoryMock)
            .around(coroutineRule)
            .apply(super.apply(base, description), description)
    }

    val userSource = MutableLiveData<UserEntity>()

    private fun setupUserSource(targetId: UserId) {
        with(userRepositoryMock) {
            setupResponseWithVerify({ mock.getUserSource(targetId) }, userSource)
        }
    }

    private val relationshipSource = MutableLiveData<Relationship>()

    fun setupRelation(targetId: UserId, response: Relationship? = null) {
        relationshipSource.value = response
        relationshipRepositoryMock.setupResponseWithVerify(
            { relationshipRepository.getRelationshipSource(targetId) }, relationshipSource
        )
        relationshipRepositoryMock.coSetupResponseWithVerify(
            { relationshipRepository.findRelationship(targetId) }, response
        )
    }
}

private fun relationship(
    givenFollowing: Boolean = false,
    givenBlocking: Boolean = false,
    givenMuting: Boolean = false,
    givenWantRetweets: Boolean = false
): Relationship = mockk<Relationship>().apply {
    every { following } returns givenFollowing
    every { blocking } returns givenBlocking
    every { muting } returns givenMuting
    every { wantRetweets } returns givenWantRetweets
}
