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

package com.freshdigitable.udonroad2.data

import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.UserId
import kotlinx.coroutines.flow.Flow

interface AppSettingDataSource {
    val currentUserId: UserId?
    val currentUserIdSource: Flow<UserId>
    val registeredUserIdsSource: Flow<Set<UserId>>

    suspend fun updateCurrentUser(accessToken: AccessTokenEntity)

    interface Local : AppSettingDataSource
    interface Remote : AppSettingDataSource
}
