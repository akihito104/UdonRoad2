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
                stateModelRule.navDelegate.dispatchNavHostNavigate(match {
                    it is TimelineEvent.Navigate.Timeline && it.owner.query == QueryType.Oauth
                })
            }
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
        fun onBackPressed_then_UnselectedEventDispatched(): Unit = with(rule) {
            // setup
            val dispatcherObserver = stateModelRule.dispatcher.emitter.test()

            // exercise
            sut.onBackPressed()

            // verify
            dispatcherObserver
                .assertValueCount(1)
                .assertValueAt(0) { it is TimelineEvent.TweetItemSelection.Unselected }
        }
    }
}

class MainViewModelTestRule(
    val stateModelRule: MainActivityStateModelTestRule = MainActivityStateModelTestRule()
) : TestWatcher() {
    val sut: MainViewModel = MainViewModel(stateModelRule.dispatcher, stateModelRule.sut)

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
