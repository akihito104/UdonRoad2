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
import com.freshdigitable.fabshortcut.ShortcutViewHolder
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.main.menuItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.test_common.MatcherScopedSuspendBlock
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    class WhenInitWithCachedUserEntity {
        @get:Rule
        val rule = UserViewModelTestRule()

        @Test
        fun initialValue(): Unit = with(rule) {
            // verify
            assertThat(sut.state.value?.user).isEqualTo(user)
            assertThat(sut.state.value?.relationship).isEqualTo(relationship)
            assertThat(sut.relationshipMenuItems.value)
                .isEqualTo(setOf(FOLLOW, BLOCK, MUTE, REPORT_SPAM))
            assertThat(sut.state.value?.shortcutMode).isEqualTo(ShortcutViewHolder.Mode.HIDDEN)
            assertThat(sut.state.value?.titleAlpha).isEqualTo(0)
        }

        @Test
        fun setAppbarScrollRate(): Unit = with(rule) {
            // exercise
            sut.scrollAppbar.dispatch(1f)

            // verify
            assertThat(sut.state.value?.titleAlpha).isEqualTo(1f)
        }

        @Test
        fun fabVisible_dispatchedSelectedEvent_then_fabVisibleIsTrue(): Unit = with(rule) {
            // setup
            sut.changePage.dispatch(0)

            // exercise
            selectedItemRepository.put(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline(targetId)), TweetId(10000)
                )
            )

            // verify
            assertThat(sut.state.value?.shortcutMode).isEqualTo(ShortcutViewHolder.Mode.FAB)
        }
    }

    class WhenInitWithFetchingUserEntity {
        @get:Rule
        val rule = UserViewModelTestRule(
            beforeSut = {
                setupUserSource()
                setupGetUser(targetId, user)
                setupRelationshipSource()
                setupFindRelationship(relationship())
            }
        )

        @Test
        fun getUserIsCalledOnInit(): Unit = with(rule) {
            // verify
            assertThat(sut.state.value?.user).isEqualTo(user)
        }
    }

    class WhenInitWithException {
        @get:Rule
        val rule = UserViewModelTestRule(
            beforeSut = {
                setupUserSource()
                userRepositoryMock.run {
                    coSetupThrowWithVerify({ mock.getUser(targetId) }, IOException())
                }
                setupRelationshipSource()
            }
        )

        @Test
        fun flowFeedbackMessageOnInit(): Unit = with(rule) {
            // verify
            assertThat(sut.state.value?.user).isNull()
            assertThat(feedbackMessages).hasSize(1)
        }
    }

    class WhenItemSelected {
        @get:Rule
        val rule = UserViewModelTestRule()

        @Before
        fun setup(): Unit = with(rule) {
            sut.changePage.dispatch(0)
            selectedItemRepository.put(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline(targetId)), TweetId(10000)
                )
            )
        }

        @Test
        fun fabVisible_changeCurrentPage_then_fabVisibleIsFalse(): Unit = with(rule) {
            // exercise
            coroutineRule.runBlockingTest {
                sut.changePage.dispatch(1)
            }
            // verify
            assertThat(sut.state.value?.shortcutMode).isEqualTo(ShortcutViewHolder.Mode.HIDDEN)
        }

        @Test
        fun fabVisible_returnToPage_then_fabVisibleIsTrue(): Unit = with(rule) {
            // setup
            sut.changePage.dispatch(1)

            // exercise
            sut.changePage.dispatch(0)

            // verify
            assertThat(sut.state.value?.shortcutMode).isEqualTo(ShortcutViewHolder.Mode.FAB)
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
                    { relationshipRepository.addSpam(targetId) }
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
            assertThat(feedbackMessages).hasSize(1)
            assertThat(feedbackMessages[0]).isEqualTo(param.expectedMessage)
        }
    }

    @RunWith(Parameterized::class)
    class WhenRelationshipUpdated(private val param: Param) {
        @get:Rule
        val rule = UserViewModelTestRule()

        data class Param(
            val givenRelationship: Relationship?,
            val menuSet: Iterable<RelationshipMenu>,
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
            // exercise
            relationshipSource.value = param.givenRelationship

            // verify
            assertThat(sut.relationshipMenuItems.value).containsExactlyElementsIn(param.menuSet)
        }
    }
}

@ExperimentalCoroutinesApi
class UserViewModelTestRule(
    private val beforeSut: UserViewModelTestRule.() -> Unit = {
        setupUserSource(user)
        setupRelationshipSource()
        setupFindRelationship(relationship)
    },
) : TestWatcher() {
    val targetId = UserId(1000)
    val relationship = relationship()
    private val targetUser: TweetUserItem = mockk<TweetUserItem>().apply {
        every { id } returns targetId
        every { screenName } returns "user1"
    }
    val user = mockk<UserEntity>().apply {
        every { id } returns targetId
    }
    val userRepositoryMock = MockVerified.create<UserDataSource>()
    val relationshipRepositoryMock = MockVerified.create<RelationshipRepository>()
    val relationshipRepository: RelationshipRepository = relationshipRepositoryMock.mock
    val selectedItemRepository = SelectedItemRepository()
    val coroutineRule = CoroutineTestRule()
    private val eventCollector = ObserverEventCollector(coroutineRule)

    val sut: UserViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        val viewStates = UserViewModelSource(
            targetUser,
            UserActions(targetUser, eventDispatcher),
            userRepositoryMock.mock,
            relationshipRepository,
            selectedItemRepository,
            ListOwnerGenerator.create(),
        )
        UserViewModel(eventDispatcher, viewStates)
    }
    val feedbackMessages: List<FeedbackMessage>
        get() = eventCollector.nonNullEventsOf(sut.feedbackMessage)

    override fun starting(description: Description?) {
        super.starting(description)
        beforeSut()
        setupSut()
    }

    private fun setupSut() {
        sut.changePage.dispatch(0)
        sut.scrollAppbar.dispatch(0f)
        eventCollector.setupForActivate {
            addAll(sut.state, sut.relationshipMenuItems)
            addAll(sut.pages, sut.feedbackMessage)
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement =
        RuleChain.outerRule(eventCollector)
            .around(userRepositoryMock)
            .around(relationshipRepositoryMock)
            .apply(super.apply(base, description), description)

    val userSource: MutableStateFlow<UserEntity?> = MutableStateFlow(null)
    val relationshipSource: MutableStateFlow<Relationship?> = MutableStateFlow(null)

    fun setupUserSource(init: UserEntity? = null) {
        userRepositoryMock.run {
            userSource.value = init
            setupResponseWithVerify({ mock.getUserSource(targetId) }, userSource)
        }
    }

    fun setupGetUser(targetId: UserId, res: UserEntity) {
        userRepositoryMock.run {
            coSetupResponseWithVerify(
                { mock.getUser(targetId) },
                res,
                alsoOnAnswer = { userSource.value = res }
            )
        }
    }

    fun setupRelationshipSource(init: Relationship? = null) {
        relationshipRepositoryMock.run {
            relationshipSource.value = init
            setupResponseWithVerify(
                { mock.getRelationshipSource(targetId) }, relationshipSource
            )
        }
    }

    fun setupFindRelationship(relationship: Relationship? = null) {
        relationshipRepositoryMock.run {
            coSetupResponseWithVerify(
                { mock.findRelationship(targetId) },
                relationship,
                alsoOnAnswer = { relationshipSource.value = relationship }
            )
        }
    }
}

private fun relationship(
    givenFollowing: Boolean = false,
    givenBlocking: Boolean = false,
    givenMuting: Boolean = false,
    givenWantRetweets: Boolean = false,
): Relationship = mockk<Relationship>().apply {
    every { following } returns givenFollowing
    every { blocking } returns givenBlocking
    every { muting } returns givenMuting
    every { wantRetweets } returns givenWantRetweets
}
