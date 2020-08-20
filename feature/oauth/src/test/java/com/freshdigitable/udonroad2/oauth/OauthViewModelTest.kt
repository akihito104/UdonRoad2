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

import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.impl.DispatcherProvider
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineExceptionHandler
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.Serializable

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class OauthViewModelTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun notNull() {
        val dataSource = OauthDataSource(ApplicationProvider.getApplicationContext())
        val repository = mockk<OAuthTokenRepository>()
        val dispatcher = EventDispatcher()
        val handle = mockk<OauthSavedStates>().apply {
            every { requestTokenItem } returns MutableLiveData()
        }
        val sut = OauthViewModel(
            dataSource,
            repository,
            dispatcher,
            handle,
            coroutineRule.coroutineContextProvider
        )

        assertThat(sut).isNotNull()
    }

    @Test
    fun onLoginClicked(): Unit = coroutineRule.runBlockingTest {
        // setup
        val dataSource = OauthDataSource(ApplicationProvider.getApplicationContext())
        val repository = mockk<OAuthTokenRepository>().apply {
            val token = object : RequestTokenItem {
                override val token: Serializable
                    get() = TODO("Not yet implemented")
                override val authorizationUrl: String = "http://localhost"
            }
            coEvery { getRequestTokenItem() } returns token
        }
        val dispatcher = EventDispatcher()
        val handle = mockk<OauthSavedStates>().apply {
            every { requestTokenItem } returns MutableLiveData()
            every { setToken(match { it.authorizationUrl == "http://localhost" }) } just runs
        }
        val sut = OauthViewModel(
            dataSource,
            repository,
            dispatcher,
            handle,
            coroutineRule.coroutineContextProvider
        )
        val test = dispatcher.emitter.test()

        // exercise
        sut.onLoginClicked()

        // verify
        assertThat(sut).isNotNull()
        coVerify { repository.getRequestTokenItem() }
        verify { handle.setToken(match { it.authorizationUrl == "http://localhost" }) }
        test.assertOf {
            it.assertValueAt(0) { actual -> actual is OauthEvent.OauthRequested }
        }
    }
}

@ExperimentalCoroutinesApi
class CoroutineTestRule : TestWatcher() {
    val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val exceptionHandler = TestCoroutineExceptionHandler()
    val coroutineContextProvider = DispatcherProvider(
        testCoroutineDispatcher,
        testCoroutineDispatcher,
        exceptionHandler
    )

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
        testCoroutineDispatcher.cleanupTestCoroutines()
        exceptionHandler.cleanupTestCoroutines()
    }

    fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) {
        testCoroutineDispatcher.runBlockingTest(block)
    }
}
