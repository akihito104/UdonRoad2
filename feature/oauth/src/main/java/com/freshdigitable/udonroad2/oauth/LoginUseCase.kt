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

import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.model.UserId
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val appSettingRepository: AppSettingDataSource,
    private val oAuthTokenRepository: OAuthTokenDataSource,
    private val userRepository: UserDataSource,
) {
    suspend operator fun invoke(userId: UserId) {
        val token = requireNotNull(oAuthTokenRepository.findUserAccessTokenEntity(userId))
        appSettingRepository.updateCurrentUser(token)
        val verifiedUser = oAuthTokenRepository.verifyCredentials()
        userRepository.addUser(verifiedUser)
    }

    companion object {
        suspend fun LoginUseCase.invokeOnLaunchApp() {
            val loginAccount = appSettingRepository.loginAccountOnLaunch
            if (loginAccount != null) {
                invoke(loginAccount)
            } else {
                appSettingRepository.currentUserId?.let { invoke(it) }
            }
        }
    }
}
