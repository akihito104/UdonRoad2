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

package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TweetRepositoryRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TweetDetailViewModelTest {
    @get:Rule
    val tweetRepositoryRule = TweetRepositoryRule()

    @get:Rule
    val executorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityEventDelegate = MockVerified.create<ActivityEventDelegate>()

    private val sut: TweetDetailViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        val actions = TweetDetailActions(eventDispatcher)
        TweetDetailViewModel(
            eventDispatcher,
            TweetDetailViewStates(actions, activityEventDelegate.mock),
            tweetRepositoryRule.mock
        )
    }

    @Before
    fun setup() {
        sut.tweetItem.observeForever { }
    }

    @Test
    fun initialState() {
        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.tweetItem.value).isNull()
    }

    @Test
    fun showTweetItem_beforeItemIsFound_tweetItemHasNoValue() {
        // setup
        val tweetId = TweetId(1000)
        tweetRepositoryRule.setupShowTweet(tweetId, MutableLiveData(null))

        // exercise
        sut.showTweetItem(tweetId)

        // verify
        assertThat(sut.tweetItem.value).isNull()
    }

    @Test
    fun showTweetItem_whenItemIsFound_then_tweetItemHasItem() {
        // setup
        val tweetId = TweetId(1000)
        val response = MutableLiveData<TweetListItem?>()
        tweetRepositoryRule.setupShowTweet(tweetId, response)

        // exercise
        sut.showTweetItem(tweetId)
        response.value = mockk()

        // verify
        assertThat(sut.tweetItem.value).isNotNull()
    }

    @Test
    fun onOriginalUserClicked_navigationDelegateIsCalled() {
        // setup
        val tweetId = TweetId(1000)
        val originalUser = mockk<TweetingUser>().apply {
            every { id } returns UserId(3000)
        }
        val tweet = mockk<TweetListItem>().apply {
            every { originalId } returns tweetId
            every { this@apply.originalUser } returns originalUser
        }
        tweetRepositoryRule.setupShowTweet(tweetId, MutableLiveData(tweet))
        every { activityEventDelegate.mock.dispatchNavHostNavigate(any()) } just runs
        sut.showTweetItem(tweetId)

        // exercise
        sut.onOriginalUserClicked()

        // verify
        assertThat(sut.tweetItem.value).isNotNull()
        verify {
            activityEventDelegate.mock.dispatchNavHostNavigate(
                TimelineEvent.Navigate.UserInfo(originalUser)
            )
        }
    }
}
