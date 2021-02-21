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

import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.RequestTokenItem
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.test_common.MockVerified
import io.mockk.mockk
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.Serializable

class OAuthTokenRepositoryRule(
    private val appSettingRule: AppSettingRepositoryRule = AppSettingRepositoryRule(),
) : TestRule {
    private val mockVerified: MockVerified<OAuthTokenDataSource> = MockVerified.create()
    val mock: OAuthTokenDataSource = mockVerified.mock
    val appSettingMock: AppSettingRepository = appSettingRule.rule.mock

    fun setupLogin(userId: UserId) {
        val token = mockk<AccessTokenEntity>()
        mockVerified.run {
            coSetupResponseWithVerify({ mock.findUserAccessTokenEntity(userId) }, token)
        }
        appSettingRule.setupUpdateCurrentUserId(token)
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

    fun setupVerifyCredentials(user: UserEntity) {
        mockVerified.coSetupResponseWithVerify({ mock.verifyCredentials() }, user)
    }

    override fun apply(base: Statement, description: Description): Statement =
        RuleChain.outerRule(mockVerified)
            .around(appSettingRule)
            .apply(base, description)

    companion object {
        val requestTokenItem = object : RequestTokenItem {
            override val authorizationUrl: String = "http://localhost"
            override val token: Serializable get() = TODO("Not yet implemented")
        }
    }
}
