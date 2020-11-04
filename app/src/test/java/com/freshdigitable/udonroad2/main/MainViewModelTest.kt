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
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.input.TweetInputEvent
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.shortcut.SelectedItemShortcut
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(Enclosed::class)
class MainViewModelTest {
    class WhenInit {
        @get:Rule
        val rule = MainViewModelTestRule()

        @Test
        fun initialEvent_withNull_then_dispatchNavigateIsCalledWithOauth(): Unit = with(rule) {
            // setup
            stateModelRule.oauthTokenRepositoryMock.setupCurrentUserId(null)

            // exercise
            sut.initialEvent(null)

            // verify
            verify {
                stateModelRule.navDelegate.dispatchNavHostNavigate(
                    match {
                        it is TimelineEvent.Navigate.Timeline && it.owner.query == QueryType.Oauth
                    }
                )
            }
        }

        @Test
        fun navIconType_WhenInTopLevelDestination_then_navIconIsMenu(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(true)

                // verify
                assertThat(sut.navIconType.value).isEqualTo(NavigationIconType.MENU)
            }

        @Test
        fun navIconType_WhenNotInTopLevelDestination_then_navIconIsUp(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(false)

                // verify
                assertThat(sut.navIconType.value).isEqualTo(NavigationIconType.UP)
            }

        @Test
        fun navIconType_WhenInTopLevelDestinationAndTweetInputIsExpanded_then_navIconIsClose(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(true)
                stateModelRule.isExpandedSource.value = true

                // verify
                assertThat(sut.navIconType.value).isEqualTo(NavigationIconType.CLOSE)
            }

        @Test
        fun navIconType_WhenNotInTopLevelDestinationAndTweetInputIsExpanded_then_navIconIsClose(): Unit =
            with(rule) {
                // setup
                stateModelRule.navDelegateRule.setIsInTopLevelDestination(false)
                stateModelRule.isExpandedSource.value = true

                // verify
                assertThat(sut.navIconType.value).isEqualTo(NavigationIconType.CLOSE)
            }

        @Test
        fun collapseTweetInput_whenTweetInputIsExpanded_then_dispatchCancelEvent(): Unit =
            with(rule) {
                // setup
                val eventObserver = stateModelRule.dispatcher.emitter.test()
                stateModelRule.isExpandedSource.value = true

                // exercise
                sut.collapseTweetInput()

                // verify
                eventObserver.assertValueCount(1)
                    .assertValueAt(0) { it is TweetInputEvent.Cancel }
            }

        @get:Rule
        val expectedException: ExpectedException = ExpectedException.none()

        @Test
        fun collapseTweetInput_whenTweetInputIsNotExpanded_then_throwIllegalStateException(): Unit =
            with(rule) {
                // setup
                stateModelRule.isExpandedSource.value = false
                expectedException.expect(IllegalStateException::class.java)

                // exercise
                sut.collapseTweetInput()
            }
    }

    class WhenItemSelected {
        @get:Rule
        val rule = MainViewModelTestRule()

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
            val dispatcherObserver = stateModelRule.dispatcher.emitter.test()

            // exercise
            sut.onFabMenuSelected(menuItem(R.id.iffabMenu_main_fav))

            // verify
            assertThat(sut.isFabVisible.value).isTrue()
            dispatcherObserver
                .assertValueCount(1)
                .assertValueAt(0) { it is SelectedItemShortcut.Like }
        }

        @Test
        fun onBackPressed_then_dispatchUnselectedEvent(): Unit = with(rule) {
            // setup
            val dispatcherObserver = stateModelRule.dispatcher.emitter.test()

            // exercise
            sut.onBackPressed()

            // verify
            dispatcherObserver
                .assertValueCount(1)
                .assertValueAt(0) { it is TimelineEvent.TweetItemSelection.Unselected }
        }

        @Test
        fun onBackPressed_whenTweetInputIsExpanded_then_dispatchInputCancelEvent(): Unit =
            with(rule) {
                // setup
                val dispatcherObserver = stateModelRule.dispatcher.emitter.test()
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
}

class MainViewModelTestRule(
    val stateModelRule: MainActivityStateModelTestRule = MainActivityStateModelTestRule()
) : TestWatcher() {
    val sut: MainViewModel = MainViewModel(stateModelRule.dispatcher, stateModelRule.sut)

    override fun starting(description: Description?) {
        super.starting(description)
        with(sut) {
            listOf(
                navIconType,
                appBarTitle,
                isTweetInputMenuVisible,
                isFabVisible,
            ).forEach { it.observeForever { } }
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(stateModelRule)
            .apply(super.apply(base, description), description)
    }
}

fun menuItem(@IdRes id: Int): MenuItem {
    return mockk<MenuItem>().apply {
        every { itemId } returns id
    }
}
