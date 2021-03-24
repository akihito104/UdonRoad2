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

package com.freshdigitable.udonroad2.main

import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.oauth.LoginUseCase
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.AppSettingRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(Enclosed::class)
class DrawerViewModelSourceTest {
    class WhenInit {
        @get:Rule
        internal val rule = DrawerViewStateSourceTestRule()

        @Test
        fun initialState(): Unit = with(rule) {
            val actual = actualStates.last()
            assertThat(actual.currentUser).isEqualTo(null)
            assertThat(actual.isAccountSwitcherOpened).isEqualTo(false)
            assertThat(actual.isOpened).isEqualTo(false)
            assertThat(actual.switchableAccounts).isEmpty()
        }

        @Test
        fun onDrawerOpened_then_isDrawerOpenedIsTrue(): Unit = with(rule) {
            coroutineRule.runBlockingTest {
                // exercise
                sut.onDrawerOpened()
            }

            // verify
            assertThat(actualStates.last().isOpened).isTrue()
        }

        @Test
        fun onDrawerClosed_then_isDrawerOpenedIsFalse(): Unit = with(rule) {
            coroutineRule.runBlockingTest {
                // setup
                sut.onDrawerOpened()

                // exercise
                sut.onDrawerClosed()
            }

            // verify
            assertThat(actualStates.last().isOpened).isFalse()
        }

        @Test
        fun onAccountSwitcherClicked_isRegisteredUsersListOpenedIsTrue(): Unit = with(rule) {
            coroutineRule.runBlockingTest {
                // exercise
                sut.onAccountSwitcherClicked()
            }

            // verify
            assertThat(actualStates.last().isAccountSwitcherOpened).isTrue()
        }

        @Test
        fun onAccountSwitcherClicked_calledTwice_then_isRegisteredUsersListOpenedIsFalse(): Unit =
            with(rule) {
                coroutineRule.runBlockingTest {
                    // exercise
                    sut.onAccountSwitcherClicked()
                    sut.onAccountSwitcherClicked()
                }

                // verify
                assertThat(actualStates.last().isAccountSwitcherOpened).isFalse()
            }
    }

    class WhenHasCurrentUserId {
        @get:Rule
        internal val rule = DrawerViewStateSourceTestRule()

        @Before
        fun setup(): Unit = with(rule) {
            setupGetUserSource(authenticatedUser)
            coroutineRule.runBlockingTest {
                appSettingRepositoryRule.currentUserIdSource.value = authenticatedUser
                appSettingRepositoryRule.registeredUserIdsSource.send(setOf(authenticatedUser))
            }
        }

        @Test
        fun init(): Unit = with(rule) {
            assertThat(actualStates.last().currentUser?.id).isEqualTo(authenticatedUser)
            assertThat(actualStates.last().switchableAccounts).isEmpty()
        }

        @Test
        fun switchableRegisteredUsers_addedNewUser_then_switchableTo1User(): Unit = with(rule) {
            // setup
            val userId = UserId(30000)
            val userEntity = mockk<UserEntity>().also {
                every { it.id } returns userId
                every { it.screenName } returns "user30000"
            }
            setupGetUser(userId, userEntity)
            coroutineRule.runBlockingTest {
                appSettingRepositoryRule.registeredUserIdsSource.send(
                    setOf(authenticatedUser, userId)
                )
            }

            // verify
            assertThat(actualStates.last().currentUser?.id).isEqualTo(authenticatedUser)
            assertThat(actualStates.last().switchableAccounts).hasSize(1)
        }

        @Test
        fun onDrawerClosed_accountSwitcherIsOpened_then_isRegisteredUserListOpenedIsFalse(): Unit =
            with(rule) {
                coroutineRule.runBlockingTest {
                    // setup
                    sut.onDrawerOpened()
                    sut.onAccountSwitcherClicked()

                    // exercise
                    sut.onDrawerClosed()
                }

                // verify
                assertThat(actualStates.last().isOpened).isFalse()
                assertThat(actualStates.last().isAccountSwitcherOpened).isFalse()
            }

        @Test
        fun onDrawerMenuItemClicked(): Unit = with(rule) {
            // setup
            sut.onDrawerOpened()

            // exercise
            val actualConsumed = sut.onDrawerMenuItemClicked(
                R.id.menu_group_drawer_default,
                R.id.menu_item_drawer_lists,
                "lists"
            )

            // verify
            assertThat(actualConsumed).isTrue()
            assertThat(actualStates.last().isOpened).isFalse()
            assertThat(actualStates.last().isAccountSwitcherOpened).isFalse()
            assertThat(navigationEventActual.last())
                .isInstanceOf(TimelineEvent.Navigate.Timeline::class.java)
            val event = navigationEventActual.last() as TimelineEvent.Navigate.Timeline
            assertThat(event.owner.query)
                .isInstanceOf(QueryType.CustomTimelineListQueryType.Ownership::class.java)
            assertThat(event.owner.query.userId).isEqualTo(authenticatedUser)
        }
    }
}

class DrawerViewStateSourceTestRule(
    private val dispatcher: EventDispatcher = EventDispatcher(),
    internal val appSettingRepositoryRule: AppSettingRepositoryRule = AppSettingRepositoryRule(),
    private val oAuthTokenRepositoryRule: OAuthTokenRepositoryRule = OAuthTokenRepositoryRule(
        appSettingRepositoryRule
    ),
    val coroutineRule: CoroutineTestRule = CoroutineTestRule(),
    private val isStateCollected: Boolean = true,
) : TestWatcher() {
    val authenticatedUser = UserId(1000)

    private val coroutineScope = CoroutineScope(coroutineRule.coroutineContextProvider.mainContext)
    private val userRepository = MockVerified.create<UserDataSource>()

    internal val sut: DrawerViewModelSource by lazy {
        DrawerViewModelSource(
            DrawerActions(dispatcher),
            LoginUseCase(
                appSettingRepositoryRule.mock,
                oAuthTokenRepositoryRule.mock,
                userRepository.mock
            ),
            ListOwnerGenerator.create(),
            appSettingRepositoryRule.mock,
            userRepository.mock
        )
    }
    internal lateinit var actualStates: List<DrawerViewModel.State>
    internal lateinit var navigationEventActual: List<NavigationEvent>

    override fun starting(description: Description?) {
        super.starting(description)
        appSettingRepositoryRule.apply {
            setupCurrentUserIdSource()
            setupRegisteredUserIdsSource(setOf())
        }
        if (isStateCollected) {
            actualStates = sut.state.testCollect(coroutineScope)
            navigationEventActual = sut.navEventSource.testCollect(coroutineScope)
        }
    }

    fun setupGetUserSource(userId: UserId) {
        val userEntity = mockk<UserEntity>().also {
            every { it.id } returns userId
        }
        userRepository.run {
            setupResponseWithVerify({ mock.getUserSource(userId) }, flow { emit(userEntity) })
        }
    }

    fun setupGetUser(userId: UserId, userEntity: UserEntity) {
        userRepository.run {
            coSetupResponseWithVerify({ mock.getUser(userId) }, userEntity)
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement =
        RuleChain.outerRule(coroutineRule)
            .around(oAuthTokenRepositoryRule)
            .around(userRepository)
            .apply(super.apply(base, description), description)
}
