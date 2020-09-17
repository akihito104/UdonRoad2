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
import com.freshdigitable.udonroad2.main.menuItem
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
import com.freshdigitable.udonroad2.test_common.MatcherScopedBlock
import com.freshdigitable.udonroad2.test_common.MockVerified
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
    class WhenRelationshipMenuIsSelected(private val param: Param) {
        @get:Rule
        val rule = UserViewModelTestRule()

        data class Param(
            @IdRes val menuId: Int,
            val text: String,
            val block: UserViewModelTestRule.() -> MatcherScopedBlock<Unit>
        ) {
            override fun toString(): String = text
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun params(): List<Param> = listOf(
                Param(R.id.action_follow, "follow") {
                    { relationshipRepository.updateFollowingStatus(targetId, true) }
                },
                Param(R.id.action_unfollow, "unfollow") {
                    { relationshipRepository.updateFollowingStatus(targetId, false) }
                },
                Param(R.id.action_mute, "mute") {
                    { relationshipRepository.updateMutingStatus(targetId, true) }
                },
                Param(R.id.action_unmute, "unmute") {
                    { relationshipRepository.updateMutingStatus(targetId, false) }
                },
                Param(R.id.action_block, "block") {
                    { relationshipRepository.updateBlockingStatus(targetId, true) }
                },
                Param(R.id.action_unblock, "unblock") {
                    { relationshipRepository.updateBlockingStatus(targetId, false) }
                },
                Param(R.id.action_block_retweet, "block_retweet") {
                    { relationshipRepository.updateWantRetweetStatus(targetId, false) }
                },
                Param(R.id.action_unblock_retweet, "want_retweet") {
                    { relationshipRepository.updateWantRetweetStatus(targetId, true) }
                },
                Param(R.id.action_r4s, "spam") {
                    { relationshipRepository.reportSpam(targetId) }
                },
            )
        }

        @Test
        fun test(): Unit = with(rule) {
            // setup
            relationshipRepositoryMock.setupResponseWithVerify(param.block(this), Unit)

            // exercise
            sut.onOptionsItemSelected(menuItem(param.menuId))
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
                return "relationship:" + (givenRelationship?.let {
                    "{following:${it.following}, blocking:${it.blocking}, " +
                        "muting:${it.muting}, wantRetweets:${it.wantRetweets}}"
                } ?: "null") +
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
        }

        @Test
        fun test(): Unit = with(rule) {
            // setup
            setupRelation(targetId, param.givenRelationship)

            // exercise
            userSource.value = mockk<User>().apply {
                every { id } returns targetId
            }

            // verify
            assertThat(sut.relationshipMenuItems.value).containsExactlyElementsIn(param.menuSet)
        }
    }
}

class UserViewModelTestRule : TestWatcher() {
    val targetId = UserId(1000)
    private val targetUser: TweetingUser = mockk<TweetingUser>().apply {
        every { id } returns targetId
        every { screenName } returns "user1"
    }
    private val userRepository = MockVerified.create<UserRepository>()
    val relationshipRepositoryMock = MockVerified.create<RelationshipRepository>()
    val relationshipRepository: RelationshipRepository = relationshipRepositoryMock.mock
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
            .around(userRepository)
            .around(relationshipRepositoryMock)
            .apply(super.apply(base, description), description)
    }

    val userSource = MutableLiveData<User>()

    private fun setupUser(targetId: UserId) {
        with(userRepository) {
            setupResponseWithVerify({ mock.getUser(targetId) }, userSource)
        }
    }

    private val relationshipSource = MutableLiveData<Relationship>()

    fun setupRelation(targetId: UserId, response: Relationship? = null) {
        relationshipSource.value = response
        relationshipRepositoryMock.setupResponseWithVerify(
            { relationshipRepository.findRelationship(targetId) }, relationshipSource
        )
    }
}
