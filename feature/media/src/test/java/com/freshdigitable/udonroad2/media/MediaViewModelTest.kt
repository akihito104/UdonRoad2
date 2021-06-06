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
import com.freshdigitable.fabshortcut.FlingFAB
import com.freshdigitable.udonroad2.data.impl.MediaRepository
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.shortcut.ShortcutActions
import com.freshdigitable.udonroad2.shortcut.ShortcutViewModelSource
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.TweetRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@ExperimentalCoroutinesApi
class MediaViewModelTest {
    private val coroutineRule = CoroutineTestRule()
    private val tweetRepositoryRule = TweetRepositoryRule()
    private val mediaRepositoryRule = MockVerified.create<MediaRepository>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(coroutineRule)
        .around(InstantTaskExecutorRule())
        .around(tweetRepositoryRule)
        .around(mediaRepositoryRule)

    private val mediaEntitySource = MutableSharedFlow<List<MediaEntity>>()
    private val tweetListItem: TweetListItem = TweetListItem.createMock(
        originalTweetId = TweetId(1000),
        body = TweetElement.createMock(TweetId(1000), media = listOf(mockk())),
    )
    private val sut: MediaViewModel by lazy {
        val eventDispatcher = EventDispatcher()
        val viewStates = MediaViewModelViewStates(
            tweetListItem.originalId,
            0,
            MediaViewModelActions(eventDispatcher),
            mediaRepositoryRule.mock,
            tweetRepositoryRule.mock,
        )
        MediaViewModel(
            viewStates,
            ShortcutViewModelSource(
                ShortcutActions(eventDispatcher),
                tweetRepositoryRule.mock,
                ListOwnerGenerator.create(),
                AppExecutor(dispatcher = coroutineRule.coroutineContextProvider),
            )
        )
    }

    @Before
    fun setup() {
        mediaRepositoryRule.setupResponseWithVerify(
            { mediaRepositoryRule.mock.getMediaItemSource(tweetListItem.originalId) },
            mediaEntitySource
        )
        with(sut) {
            listOf(state, mediaItems, systemUiVisibility, shortcutState).forEach {
                it.observeForever {}
            }
        }
    }

    @Test
    fun initialValue() {
        // verify
        assertThat(sut).isNotNull()
        assertThat(sut.mediaItems.value).isEmpty()
        assertThat(sut.state.value?.currentPosition).isNull()
        assertThat(sut.systemUiVisibility.value).isEqualTo(SystemUiVisibility.SHOW)
        assertThat(sut.shortcutState.value?.mode).isEqualTo(FlingFAB.Mode.FAB)
        assertThat(sut.state.value?.isPictureEnlarged).isFalse()
    }

    @Test
    fun setTweetId_foundTweetItem_mediaItemsHasItem() {
        // exercise
        coroutineRule.runBlockingTest {
            mediaEntitySource.emit(listOf(mockk()))
        }

        // verify
        assertThat(sut.mediaItems.value).hasSize(1)
        assertThat(sut.state.value?.currentPosition).isEqualTo(0)
    }

    @Test
    fun toggleUiVisibility() {
        // exercise
        sut.toggleSystemUiVisibility.dispatch()

        // verify
        assertThat(sut.systemUiVisibility.value).isEqualTo(SystemUiVisibility.HIDE)
        assertThat(sut.shortcutState.value?.mode).isEqualTo(FlingFAB.Mode.HIDDEN)
    }

    @Test
    fun isPictureEnlarged_receiveItem_then_false() {
        // setup
        coroutineRule.runBlockingTest {
            mediaEntitySource.emit(listOf(mockk()))
        }
        assertThat(sut.state.value?.isPictureEnlarged).isFalse()
    }

    @Test
    fun onScale_scale1_2_then_isPictureEnlargedIsTrue() {
        // setup
        coroutineRule.runBlockingTest {
            mediaEntitySource.emit(listOf(mockk()))
        }
        assertThat(sut.state.value?.isPictureEnlarged).isFalse()

        // exercise
        sut.onScale(1.2f, 100f, 100f)

        // verify
        assertThat(sut.state.value?.currentPosition).isEqualTo(0)
        assertThat(sut.state.value?.isPictureEnlarged).isTrue()
    }

    @Test
    fun changeCurrentPosition_callAfterOnScale_then_false() {
        // setup
        coroutineRule.runBlockingTest {
            mediaEntitySource.emit(listOf(mockk(), mockk()))
        }
        sut.onScale(1.2f, 100f, 100f)

        // exercise
        sut.changeCurrentPosition.dispatch(1)

        // verify
        assertThat(sut.state.value?.currentPosition).isEqualTo(1)
        assertThat(sut.state.value?.isPictureEnlarged).isFalse()
    }

    @Test
    fun changeCurrentPosition_goNextPageAndBack_then_isPictureEnlargedTrue() {
        // setup
        coroutineRule.runBlockingTest {
            mediaEntitySource.emit(listOf(mockk(), mockk()))
        }
        sut.onScale(1.2f, 100f, 100f)

        // exercise
        sut.changeCurrentPosition.dispatch(1)
        sut.changeCurrentPosition.dispatch(0)

        // verify
        assertThat(sut.state.value?.currentPosition).isEqualTo(0)
        assertThat(sut.state.value?.isPictureEnlarged).isTrue()
    }

    @Test
    fun changeCurrentPosition_backToScaleUpPage_then_isPictureEnlargedTrue() {
        // setup
        coroutineRule.runBlockingTest {
            mediaEntitySource.emit(listOf(mockk(), mockk()))
        }
        sut.onScale(1.2f, 100f, 100f)
        sut.changeCurrentPosition.dispatch(1)
        sut.onScale(1.5f, 100f, 100f)

        // exercise
        sut.changeCurrentPosition.dispatch(0)

        // verify
        assertThat(sut.state.value?.currentPosition).isEqualTo(0)
        assertThat(sut.state.value?.isPictureEnlarged).isTrue()
    }
}
