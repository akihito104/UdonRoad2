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

package com.freshdigitable.udonroad2.test_common

import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.rules.TestRule
import java.io.Serializable

class OAuthTokenRepositoryRule(
    val mock: OAuthTokenRepository = mockk(),
    private val mockVerified: MockVerified = MockVerified(listOf(mock))
) : TestRule by mockVerified {
    fun setupCurrentUserId(userId: Long?) {
        val id = UserId.create(userId)
        every { mock.getCurrentUserId() } returns id
        mockVerified.expected { verify { mock.getCurrentUserId() } }
        if (id != null) {
            setupLogin(id)
        }
    }

    fun setupLogin(id: UserId) {
        every { mock.login(id) } just runs
        mockVerified.expected { verify { mock.login(id) } }
    }

    fun setupGetRequestTokenItem(token: RequestTokenItem = requestTokenItem) {
        coEvery { mock.getRequestTokenItem() } returns token
        mockVerified.expected { coVerify { mock.getRequestTokenItem() } }
    }

    fun setupGetAccessToken(verifier: String, accessTokenUser: UserId) {
        coEvery { mock.getAccessToken(any(), "012345") } returns AccessTokenEntity.create(
            accessTokenUser, "token", "tokenSecret"
        )
        mockVerified.expected { coVerify { mock.getAccessToken(any(), verifier) } }
    }

    companion object {
        val requestTokenItem = object : RequestTokenItem {
            override val authorizationUrl: String = "http://localhost"
            override val token: Serializable get() = TODO("Not yet implemented")
        }
    }
}
