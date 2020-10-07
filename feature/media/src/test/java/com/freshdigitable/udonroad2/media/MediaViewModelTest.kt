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

package com.freshdigitable.udonroad2.media

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@ExperimentalCoroutinesApi
class MediaViewModelTest {
    private val coroutineRule = CoroutineTestRule()
    private val tweetRepositoryRule = TweetRepositoryRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(coroutineRule)
        .around(InstantTaskExecutorRule())
        .around(tweetRepositoryRule)

    private val sut: MediaViewModel by lazy {
        MediaViewModel(
            tweetRepositoryRule.mock,
            AppExecutor(dispatcher = coroutineRule.coroutineContextProvider),
        )
    }
    private val tweetItemSource = MutableLiveData<TweetListItem?>()
    private val tweetListItem: TweetListItem = mockk<TweetListItem>().apply {
        val tweetId = TweetId(1000)
        every { originalId } returns tweetId
        every { body } returns mockk<Tweet>().apply {
            every { id } returns tweetId
            every { mediaItems } returns listOf(mockk())
        }
    }

    @Before
    fun setup() {
        with(sut) {
            listOf(tweet, currentPosition, systemUiVisibility, isInImmersive).forEach {
                it.observeForever {}
            }
        }
    }

    @Test
    fun initialValue() {
        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.tweet.value).isNull()
        assertThat(sut.currentPosition.value).isNull()
        assertThat(sut.systemUiVisibility.value).isNotNull() // XXX
        assertThat(sut.isInImmersive.value).isNotNull() // XXX
    }

    @Test
    fun setTweetId_beforeFoundTweetItem_tweetHasNoItem() {
        // setup
        tweetRepositoryRule.setupShowTweet(TweetId(1000), tweetItemSource)

        // exercise
        sut.setTweetId(TweetId(1000))

        // verify
        assertThat(sut.tweet.value).isNull()
    }

    @Test
    fun setTweetId_foundTweetItem_tweetHasItem() {
        // setup
        tweetRepositoryRule.setupShowTweet(tweetListItem.originalId, tweetItemSource)

        // exercise
        sut.setTweetId(tweetListItem.originalId)
        tweetItemSource.value = tweetListItem

        // verify
        assertThat(sut.tweet.value).isNotNull()
        assertThat(sut.mediaItems.value).hasSize(1)
    }

    @Test
    fun setCurrentPosition_afterTweetItemLoaded_currentPositionIsSet() {
        // setup
        tweetRepositoryRule.setupShowTweet(tweetListItem.originalId, tweetItemSource)
        sut.setTweetId(tweetListItem.originalId)
        tweetItemSource.value = tweetListItem

        // exercise
        sut.setCurrentPosition(0)

        // verify
        assertThat(sut.tweet.value).isNotNull()
        assertThat(sut.mediaItems.value).hasSize(1)
        assertThat(sut.currentPosition.value).isEqualTo(0)
    }

    @Test
    fun setCurrentPosition_beforeTweetItemLoaded_currentPositionIsNull() {
        // setup
        tweetRepositoryRule.setupShowTweet(tweetListItem.originalId, tweetItemSource)
        sut.setTweetId(tweetListItem.originalId)

        // exercise
        sut.setCurrentPosition(0)

        // verify
        assertThat(sut.tweet.value).isNull()
        assertThat(sut.mediaItems.value).isNull()
        assertThat(sut.currentPosition.value).isNull()
    }

    @Test
    fun setCurrentPosition_tweetItemLoadedAfterPositionIsSet_currentPositionIsSet() {
        // setup
        tweetRepositoryRule.setupShowTweet(tweetListItem.originalId, tweetItemSource)
        sut.setTweetId(tweetListItem.originalId)

        // exercise
        sut.setCurrentPosition(0)
        tweetItemSource.value = tweetListItem

        // verify
        assertThat(sut.tweet.value).isNotNull()
        assertThat(sut.mediaItems.value).hasSize(1)
        assertThat(sut.currentPosition.value).isEqualTo(0)
    }
}
