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

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.create
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.RxExceptionHandler
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.freshdigitable.udonroad2.test_common.jvm.ObserverEventCollector
import com.freshdigitable.udonroad2.test_common.jvm.setupForActivate
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OauthViewModelTest {
    @get:Rule
    val rule: OauthViewModelTestRule = OauthViewModelTestRule()

    @Test
    fun onLoginClicked(): Unit = rule.runBlockingTest {
        // setup
        oauthRepository.setupGetRequestTokenItem()

        // exercise
        sut.authApp.dispatch()

        // verify
        dispatcherObserver.assertValueAt(0) { actual -> actual is OauthEvent.LoginClicked }
        assertThat(sut.sendPinButtonEnabled.value).isFalse()
    }

    @Test
    fun onAfterPinTextChanged(): Unit = rule.runBlockingTest {
        // setup
        oauthRepository.setupGetRequestTokenItem()

        sut.authApp.dispatch()
        assertThat(sut.sendPinButtonEnabled.value).isFalse()

        // exercise
        sut.inputPin.dispatch("012345")

        // verify
        dispatcherObserver.assertValueAt(0) { actual -> actual is OauthEvent.LoginClicked }
        assertThat(sut.sendPinButtonEnabled.value).isTrue()
    }

    @Test
    fun onSendPinClicked(): Unit = rule.runBlockingTest {
        // setup
        val user = mockk<UserEntity>().also {
            every { it.id } returns UserId(100)
        }
        oauthRepository.setupGetRequestTokenItem()
        oauthRepository.setupGetAccessToken("012345", UserId(100))
        oauthRepository.setupVerifyCredentials(user)
        setupLogin(user.id)
        userRepository.coSetupResponseWithVerify({ userRepository.mock.addUser(user) }, Unit)

        sut.authApp.dispatch()
        sut.inputPin.dispatch("012345")

        // exercise
        sut.sendPin.dispatch()

        // verify
        dispatcherObserver
            .assertValueCount(3)
            .assertValueAt(0) { actual -> actual is OauthEvent.LoginClicked }
            .assertValueAt(1) { actual -> actual is OauthEvent.PinTextChanged }
            .assertValueAt(2) { actual -> actual is OauthEvent.SendPinClicked }
        assertThat(navEvents[1]).isInstanceOf(TimelineEffect.Navigate.Timeline::class.java)
        assertThat((navEvents[1] as TimelineEffect.Navigate.Timeline).owner.query)
            .isInstanceOf(QueryType.Tweet.Timeline::class.java)
        assertThat(sut.sendPinButtonEnabled.value).isFalse()
    }
}

@ExperimentalCoroutinesApi
class OauthViewModelTestRule : TestWatcher() {
    private val coroutineRule = CoroutineTestRule()
    private val eventCollector = ObserverEventCollector(coroutineRule)
    val oauthRepository = OAuthTokenRepositoryRule()
    val userRepository = MockVerified.create<UserDataSource>()
    private val dispatcher = EventDispatcher()

    internal val sut: OauthViewModel by lazy {
        OauthViewModel(
            OauthViewModelSource(
                OauthAction(dispatcher),
                LoginUseCase(
                    oauthRepository.appSettingMock,
                    oauthRepository.mock,
                    userRepository.mock
                ),
                OauthDataSource(ApplicationProvider.getApplicationContext()),
                oauthRepository.mock,
                ListOwnerGenerator.create(),
                OauthSavedStates(SavedStateHandle()),
            )
        )
    }
    val dispatcherObserver: TestObserver<AppEvent> = dispatcher.emitter.test()
    val navEvents: List<AppEffect> get() = eventCollector.nonNullEventsOf(sut.effect)

    override fun starting(description: Description?) {
        super.starting(description)
        eventCollector.setupForActivate {
            addAll(sut.state, sut.sendPinButtonEnabled, sut.listState)
            addAll(sut.timeline)
            addActivityEventStream(sut)
        }
    }

    fun runBlockingTest(block: suspend OauthViewModelTestRule.() -> Unit) {
        coroutineRule.runBlockingTest { block() }
    }

    fun setupLogin(id: UserId) {
        oauthRepository.setupLogin(id)
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(RxExceptionHandler())
            .around(eventCollector)
            .around(oauthRepository)
            .around(userRepository)
            .apply(super.apply(base, description), description)
    }
}
