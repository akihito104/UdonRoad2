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

package com.freshdigitable.udonroad2.oauth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.oauth.LoginUseCase.Companion.invokeIfCan
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginUseCaseTest {
    private val appSettingRepository = MockVerified.create<AppSettingDataSource>()
    private val oauthRepository = MockVerified.create<OAuthTokenDataSource>()
    private val userRepository = MockVerified.create<UserDataSource>()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(appSettingRepository)
        .around(oauthRepository)
        .around(userRepository)

    private val sut =
        LoginUseCase(appSettingRepository.mock, oauthRepository.mock, userRepository.mock)

    @Test
    fun hasNoAccessToken_then_throwIllegalArgumentException() {
        // setup
        oauthRepository.run {
            coSetupResponseWithVerify({ mock.findUserAccessTokenEntity(any()) }, null)
        }

        // exercise
        val res = runBlocking {
            kotlin.runCatching {
                sut(UserId(1000))
            }
        }

        // verify
        assertThat(res.isFailure).isTrue()
        assertThat(res.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun invokeIfCan_currentUserIdIsNull_then_notInvoked() {
        // setup
        appSettingRepository.run {
            setupResponseWithVerify({ mock.currentUserId }, null)
        }

        // exercise
        runBlocking {
            sut.invokeIfCan()
        }

        coVerify(exactly = 0) { oauthRepository.mock.verifyCredentials() }
    }

    @Test
    fun invokeIfCan_hasCurrentUserId_then_login() {
        // setup
        val userId = UserId(1000)
        appSettingRepository.run {
            setupResponseWithVerify({ mock.currentUserId }, userId)
        }
        setupForLogin(userId)

        // exercise
        runBlocking {
            sut.invokeIfCan()
        }
    }

    @Test
    fun hasAccessToken_then_passVerifyCredentials() {
        // setup
        val userId = UserId(1000)
        setupForLogin(userId)

        // exercise
        runBlocking {
            sut(userId)
        }
    }

    private fun setupForLogin(userId: UserId) {
        val accessToken = mockk<AccessTokenEntity>()
        val user = mockk<UserEntity>().also {
            every { it.id } returns userId
        }
        oauthRepository.run {
            coSetupResponseWithVerify({ mock.findUserAccessTokenEntity(userId) }, accessToken)
            coSetupResponseWithVerify({ mock.verifyCredentials() }, user)
        }
        appSettingRepository.run {
            coSetupResponseWithVerify({ mock.updateCurrentUser(accessToken) }, Unit)
        }
        userRepository.run {
            coSetupResponseWithVerify({ mock.addUser(user) }, Unit)
        }
    }
}
