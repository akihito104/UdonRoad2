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

package com.freshdigitable.udonroad2.test_common.jvm

import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified
import kotlinx.coroutines.flow.flow
import org.junit.rules.TestRule
import java.io.Serializable

class OAuthTokenRepositoryRule(
    private val mockVerified: MockVerified<OAuthTokenRepository> = MockVerified.create()
) : TestRule by mockVerified {
    val mock: OAuthTokenRepository = mockVerified.mock

    fun setupCurrentUserId(userId: Long?, needLogin: Boolean = true) {
        val id = UserId.create(userId)
        mockVerified.setupResponseWithVerify({ mock.getCurrentUserId() }, id)
        if (id != null && needLogin) {
            setupLogin(id)
        }
    }

    fun setupCurrentUserIdSource(userId: Long?) {
        mockVerified.setupResponseWithVerify(
            { mock.getCurrentUserIdFlow() },
            flow { emit(UserId.create(userId)) }
        )
    }

    fun setupLogin(id: UserId) {
        mockVerified.setupResponseWithVerify({ mock.login(id) }, Unit)
    }

    fun setupGetRequestTokenItem(token: RequestTokenItem = requestTokenItem) {
        mockVerified.coSetupResponseWithVerify({ mock.getRequestTokenItem() }, token)
    }

    fun setupGetAccessToken(verifier: String, accessTokenUser: UserId) {
        mockVerified.coSetupResponseWithVerify(
            { mock.getAccessToken(any(), verifier) },
            AccessTokenEntity.create(accessTokenUser, "token", "tokenSecret")
        )
    }

    companion object {
        val requestTokenItem = object : RequestTokenItem {
            override val authorizationUrl: String = "http://localhost"
            override val token: Serializable get() = TODO("Not yet implemented")
        }
    }
}
