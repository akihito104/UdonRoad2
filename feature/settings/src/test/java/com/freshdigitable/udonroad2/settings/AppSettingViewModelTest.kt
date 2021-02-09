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

package com.freshdigitable.udonroad2.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.CoroutineTestRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

class AppSettingViewModelTest {

    private val repository = MockVerified.create<AppSettingRepository>()
    private val userRepository = MockVerified.create<UserDataSource>()
    private val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(InstantTaskExecutorRule())
        .around(coroutineTestRule)
        .around(repository)
        .around(userRepository)

    private val sut: AppSettingViewModel by lazy {
        AppSettingViewModel(
            repository.mock,
            userRepository.mock,
            AppExecutor(dispatcher = coroutineTestRule.coroutineContextProvider)
        )
    }

    @Test
    fun whenRegisteredUserIsNothing_then_registeredUserAccountIsEmpty() {
        // setup
        repository.run {
            setupResponseWithVerify({ repository.mock.registeredUserIdsSource }, emptyFlow())
        }
        sut.registeredUserAccount.observeForever { }

        // verify
        assertThat(sut.registeredUserAccount.value).isEmpty()
    }

    @Test
    fun whenRegisteredUserHas1_then_registeredUserAccountReturns1Account() {
        // setup
        val userId = UserId(1000)
        repository.run {
            setupResponseWithVerify({ mock.registeredUserIdsSource }, flowOf(setOf(userId)))
        }
        userRepository.run {
            val res = mockk<UserEntity>().also {
                every { it.id } returns userId
                every { it.screenName } returns "user1000"
            }
            coSetupResponseWithVerify({ mock.getUser(userId) }, res)
        }
        sut.registeredUserAccount.observeForever { }

        // verify
        assertThat(sut.registeredUserAccount.value).containsExactly(userId to "@user1000")
    }
}
