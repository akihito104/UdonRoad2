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

package com.freshdigitable.udonroad2.oauth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.impl.AppExecutor
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.Serializable

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OauthViewModelTest {
    private val coroutineRule = CoroutineTestRule()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(InstantTaskExecutorRule())
        .around(RxExceptionHandler())
        .around(coroutineRule)

    private val dataSource = OauthDataSource(ApplicationProvider.getApplicationContext())
    private val repository = mockk<OAuthTokenRepository>()
    private val dispatcher = EventDispatcher()
    private val savedStates = OauthSavedStates(SavedStateHandle())
    private val activityEventDelegate: ActivityEventDelegate = mockk()
    private val navDelegate: OauthNavigationDelegate =
        OauthNavigationDelegate(mockk(relaxed = true), ListOwnerGenerator(), activityEventDelegate)
    private val sut = OauthViewModel(
        dataSource, dispatcher, OauthViewStates(
            OauthAction(dispatcher),
            navDelegate,
            repository,
            savedStates,
            AppExecutor(dispatcher = coroutineRule.coroutineContextProvider)
        )
    )

    @After
    fun onTestFinished() {
        confirmVerified(repository)
    }

    @Test
    fun onLoginClicked(): Unit = coroutineRule.runBlockingTest {
        // setup
        val token = object : RequestTokenItem {
            override val token: Serializable
                get() = TODO("Not yet implemented")
            override val authorizationUrl: String = "http://localhost"
        }
        coEvery { repository.getRequestTokenItem() } returns token
        val test = dispatcher.emitter.test()
        sut.sendPinButtonEnabled.observeForever { }

        // exercise
        sut.onLoginClicked()

        // verify
        test.assertValueAt(0) { actual -> actual is OauthEvent.LoginClicked }
        coVerify { repository.getRequestTokenItem() }
        assertThat(sut.sendPinButtonEnabled.value).isFalse()
    }

    @Test
    fun onAfterPinTextChanged(): Unit = coroutineRule.runBlockingTest {
        // setup
        val token = object : RequestTokenItem {
            override val token: Serializable
                get() = TODO("Not yet implemented")
            override val authorizationUrl: String = "http://localhost"
        }
        coEvery { repository.getRequestTokenItem() } returns token
        val dispatcherObserver = dispatcher.emitter.test()
        sut.sendPinButtonEnabled.observeForever { }

        sut.onLoginClicked()
        assertThat(sut.sendPinButtonEnabled.value).isFalse()

        // exercise
        sut.onAfterPinTextChanged("012345")

        // verify
        coVerify { repository.getRequestTokenItem() }
        dispatcherObserver.assertValueAt(0) { actual -> actual is OauthEvent.LoginClicked }
        assertThat(sut.sendPinButtonEnabled.value).isTrue()
    }

    @Test
    fun onSendPinClicked(): Unit = coroutineRule.runBlockingTest {
        // setup
        val token = object : RequestTokenItem {
            override val token: Serializable
                get() = TODO("Not yet implemented")
            override val authorizationUrl: String = "http://localhost"
        }
        coEvery { repository.getRequestTokenItem() } returns token
        coEvery { repository.getAccessToken(any(), "012345") } returns AccessTokenEntity.create(
            UserId(100), "token", "tokenSecret"
        )
        every { repository.login(UserId(100)) } just runs
        val dispatcherObserver = dispatcher.emitter.test()
        sut.sendPinButtonEnabled.observeForever { }
        every { activityEventDelegate.dispatchNavHostNavigate(any()) } just runs

        sut.onLoginClicked()
        sut.onAfterPinTextChanged("012345")

        // exercise
        sut.onSendPinClicked()

        // verify
        coVerify { repository.getRequestTokenItem() }
        coVerify { repository.getAccessToken(any(), any()) }
        verify { repository.login(UserId(100)) }
        dispatcherObserver
            .assertValueCount(3)
            .assertValueAt(0) { actual -> actual is OauthEvent.LoginClicked }
            .assertValueAt(1) { actual -> actual is OauthEvent.PinTextChanged }
            .assertValueAt(2) { actual -> actual is OauthEvent.SendPinClicked }
        verify {
            activityEventDelegate.dispatchNavHostNavigate(match {
                it is TimelineEvent.Navigate.Timeline &&
                    it.owner.query is QueryType.TweetQueryType.Timeline
            })
        }
        assertThat(sut.sendPinButtonEnabled.value).isFalse()
    }
}
