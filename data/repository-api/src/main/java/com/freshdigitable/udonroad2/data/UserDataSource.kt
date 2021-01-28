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

import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserDataSource {
    fun getUserSource(id: UserId): Flow<UserEntity?>
    suspend fun getUser(id: UserId): UserEntity
    suspend fun findUser(id: UserId): UserEntity?
    suspend fun addUser(user: UserEntity)

    interface Local : UserDataSource
    interface Remote : UserDataSource
}
