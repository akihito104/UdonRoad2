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

import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.input.TweetInputEvent
import com.freshdigitable.udonroad2.main.MainViewModelTestRule.Companion.currentUser
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.test_common.jvm.testCollect
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
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
class MainViewModelTest {
    class WhenInit {
        @get:Rule
        internal val rule = MainViewModelTestRule()

        @Test
        fun initialState(): Unit = with(rule) {
            val actual = sut.drawerState.value
            assertThat(actual?.switchableAccounts).isEmpty()
            assertThat(actual?.isOpened).isFalse()
            assertThat(actual?.isAccountSwitcherOpened).isFalse()
            assertThat(actual?.currentUser).isNull()
        }

        @Test
        fun onDrawerOpened_then_isDrawerOpenedIsTrue(): Unit = with(rule) {
            coroutineRule.runBlockingTest {
                // exercise
                sut.onDrawerOpened()
            }

            // verify
            assertThat(sut.drawerState.value?.isOpened).isTrue()
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
            assertThat(sut.drawerState.value?.isOpened).isFalse()
        }

        @Test
        fun onAccountSwitcherClicked_isRegisteredUsersListOpenedIsTrue(): Unit = with(rule) {
            coroutineRule.runBlockingTest {
                // exercise
                sut.onAccountSwitcherClicked()
            }

            // verify
            assertThat(sut.drawerState.value?.isAccountSwitcherOpened).isTrue()
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
                assertThat(sut.drawerState.value?.isAccountSwitcherOpened).isFalse()
            }

        @Test
        fun initialEvent_withNull_then_dispatchNavigateIsCalledWithOauth(): Unit = with(rule) {
            // setup
            appSettingRepositoryRule.setupCurrentUserId(null)

            // exercise
            sut.initialEvent()

            // verify
            stateModelRule.assertThatNavigationEventOfTimeline(0) {
                assertThat(it.owner.query).isEqualTo(QueryType.Oauth)
            }
        }

        @Test
        fun navIconType_WhenInTopLevelDestination_then_navIconIsMenu(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(true)

                // verify
                assertThat(sut.mainState.value?.navIconType).isEqualTo(NavigationIconType.MENU)
            }

        @Test
        fun navIconType_WhenNotInTopLevelDestination_then_navIconIsUp(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(false)

                // verify
                assertThat(sut.mainState.value?.navIconType).isEqualTo(NavigationIconType.UP)
            }

        @Test
        fun navIconType_WhenInTopLevelDestinationAndTweetInputIsExpanded_then_navIconIsClose(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(true)
                stateModelRule.isExpandedSource.value = true

                // verify
                assertThat(sut.mainState.value?.navIconType).isEqualTo(NavigationIconType.CLOSE)
            }

        @Test
        fun navIconType_WhenNotInTopLevelDestinationAndTweetInputIsExpanded_then_navIconIsClose(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(false)
                stateModelRule.isExpandedSource.value = true

                // verify
                assertThat(sut.mainState.value?.navIconType).isEqualTo(NavigationIconType.CLOSE)
            }

        @Test
        fun collapseTweetInput_whenTweetInputIsExpanded_then_dispatchCancelEvent(): Unit =
            with(rule) {
                // setup
                val eventObserver = dispatcher.emitter.test()
                stateModelRule.isExpandedSource.value = true

                // exercise
                sut.collapseTweetInput()

                // verify
                eventObserver.assertValueCount(1)
                    .assertValueAt(0) { it is TweetInputEvent.Cancel }
            }

        @Test
        fun collapseTweetInput_whenTweetInputIsNotExpanded_then_throwIllegalStateException(): Unit =
            with(rule) {
                // setup
                stateModelRule.isExpandedSource.value = false

                // exercise
                Assert.assertThrows(java.lang.IllegalStateException::class.java) {
                    sut.collapseTweetInput()
                }
            }
    }

    class WhenItemSelected {
        @get:Rule
        internal val rule = MainViewModelTestRule()

        @Before
        fun setup(): Unit = with(rule) {
            stateModelRule.navDelegateRule.setupContainerState(
                MainNavHostState.Timeline(ListOwner(0, QueryType.TweetQueryType.Timeline()))
            )
            stateModelRule.selectedItemRepository.put(
                SelectedItemId(
                    ListOwner(0, QueryType.TweetQueryType.Timeline()), TweetId(200)
                )
            )
        }

        @Test
        fun onFabMenuSelected_selectedFav_then_favDispatched(): Unit = with(rule) {
            // setup
            val dispatcherObserver = dispatcher.emitter.test()

            // exercise
            sut.onShortcutMenuSelected(
                menuItem(R.id.iffabMenu_main_fav),
                sut.requireSelectedTweetId
            )

            // verify
            assertThat(sut.isFabVisible.value).isTrue()
            dispatcherObserver
                .assertValueCount(1)
                .assertValueAt(0) { it is SelectedItemShortcut.Like }
        }

        @Test
        fun onBackPressed_then_dispatchUnselectedEvent(): Unit = with(rule) {
            // setup
            val dispatcherObserver = dispatcher.emitter.test()

            // exercise
            val actual = sut.onBackPressed()

            // verify
            assertThat(actual).isTrue()
            dispatcherObserver
                .assertValueCount(1)
                .assertValueAt(0) { it is TimelineEvent.TweetItemSelection.Unselected }
        }

        @Test
        fun onBackPressed_whenTweetInputIsExpanded_then_dispatchInputCancelEvent(): Unit =
            with(rule) {
                // setup
                val dispatcherObserver = dispatcher.emitter.test()
                stateModelRule.isExpandedSource.value = true

                // exercise
                sut.onBackPressed()

                // verify
                dispatcherObserver
                    .assertValueCount(1)
                    .assertValueAt(0) { it is TweetInputEvent.Cancel }
            }

        @Test
        fun isFabVisible_tweetInputIsExpanded_then_fabIsDisappeared(): Unit = with(rule) {
            // exercise
            stateModelRule.isExpandedSource.value = true

            // verify
            assertThat(sut.isFabVisible.value).isFalse()
        }
    }

    class WhenHasCurrentUserId {
        @get:Rule
        internal val rule = MainViewModelTestRule()

        @Before
        fun setup(): Unit = with(rule) {
            drawerViewStateRule.setupGetUserSource(authenticatedUserId)
            coroutineRule.runBlockingTest {
                appSettingRepositoryRule.currentUserIdSource.value = authenticatedUserId
                appSettingRepositoryRule.registeredUserIdsSource.send(setOf(authenticatedUserId))
            }
        }

        @Test
        fun init(): Unit = with(rule) {
            assertThat(sut.drawerState.currentUser.id).isEqualTo(authenticatedUserId)
            assertThat(sut.drawerState.value?.switchableAccounts).isEmpty()
        }

        @Test
        fun switchableRegisteredUsers_addedNewUser_then_switchableTo1User(): Unit = with(rule) {
            // setup
            val userId = UserId(30000)
            val userEntity = mockk<UserEntity>().also {
                every { it.id } returns userId
                every { it.screenName } returns "user30000"
            }
            drawerViewStateRule.setupGetUser(userId, userEntity)
            coroutineRule.runBlockingTest {
                appSettingRepositoryRule.registeredUserIdsSource.send(
                    setOf(authenticatedUserId, userId)
                )
            }

            // verify
            assertThat(sut.drawerState.currentUser.id).isEqualTo(authenticatedUserId)
            assertThat(sut.drawerState.value?.switchableAccounts).hasSize(1)
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
                assertThat(sut.drawerState.value?.isOpened).isFalse()
                assertThat(sut.drawerState.value?.isAccountSwitcherOpened).isFalse()
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
            assertThat(sut.drawerState.value?.isOpened).isFalse()
            assertThat(sut.drawerState.value?.isAccountSwitcherOpened).isFalse()
            assertThat(navigationEventActual.last())
                .isInstanceOf(TimelineEvent.Navigate.Timeline::class.java)
            val event = navigationEventActual.last() as TimelineEvent.Navigate.Timeline
            assertThat(event.owner.query)
                .isInstanceOf(QueryType.CustomTimelineListQueryType.Ownership::class.java)
            assertThat(event.owner.query.userId).isEqualTo(authenticatedUserId)
        }

        @Test
        fun onCurrentUserIconClicked(): Unit = with(rule) {
            // exercise
            sut.onCurrentUserIconClicked()

            // verify
            assertThat(navigationEventActual.last())
                .isInstanceOf(TimelineEvent.Navigate.UserInfo::class.java)
            val actualEvent = navigationEventActual.last() as TimelineEvent.Navigate.UserInfo
            assertThat(actualEvent.tweetUserItem.id).isEqualTo(authenticatedUserId)
        }
    }
}

internal class MainViewModelTestRule : TestWatcher() {
    val stateModelRule: MainActivityStateModelTestRule = MainActivityStateModelTestRule()
    val dispatcher = stateModelRule.dispatcher
    val coroutineRule = stateModelRule.coroutineRule
    val appSettingRepositoryRule = stateModelRule.appSettingRepositoryRule
    val authenticatedUserId = stateModelRule.authenticatedUserId
    val drawerViewStateRule = DrawerViewStateSourceTestRule(
        dispatcher,
        appSettingRepositoryRule,
        stateModelRule.oauthTokenRepository,
        coroutineRule
    )

    val sut: MainViewModel by lazy {
        MainViewModel(dispatcher, stateModelRule.sut, drawerViewStateRule.sut)
    }
    lateinit var navigationEventActual: List<NavigationEvent>

    override fun starting(description: Description?) {
        super.starting(description)
        with(sut) {
            listOf(
                mainState,
                appBarTitle,
                isTweetInputMenuVisible,
                isFabVisible,
                drawerState
            ).forEach { it.observeForever { } }
        }
        navigationEventActual = sut.navigationEvent.testCollect(stateModelRule.coroutineScope)
    }

    override fun apply(base: Statement?, description: Description?): Statement =
        RuleChain.outerRule(stateModelRule)
            .around(drawerViewStateRule)
            .apply(super.apply(base, description), description)

    companion object {
        val LiveData<DrawerViewModel.State>.currentUser: TweetUserItem
            get() = requireNotNull(this.value?.currentUser)
    }
}

fun menuItem(@IdRes id: Int): MenuItem {
    return mockk<MenuItem>().apply {
        every { itemId } returns id
    }
}
