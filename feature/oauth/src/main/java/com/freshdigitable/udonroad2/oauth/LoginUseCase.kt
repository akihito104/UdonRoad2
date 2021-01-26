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

import com.freshdigitable.udonroad2.data.UserRepository
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.model.UserId
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    internal val tokenRepository: OAuthTokenRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(userId: UserId? = tokenRepository.getCurrentUserId()) {
        val user = tokenRepository.login(checkNotNull(userId))
        userRepository.addUser(user)
    }

    companion object {
        suspend fun LoginUseCase.invokeIfCan() {
            if (tokenRepository.getCurrentUserId() != null) {
                invoke()
            }
        }
    }
}
