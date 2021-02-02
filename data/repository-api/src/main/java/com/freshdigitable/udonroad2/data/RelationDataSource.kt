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
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow

interface RelationDataSource {

    suspend fun addSpam(userId: UserId): UserEntity

    fun getRelationshipSource(targetUserId: UserId): Flow<Relationship?>
    suspend fun findRelationship(targetUserId: UserId): Relationship?

    suspend fun updateFollowingStatus(targetUserId: UserId, isFollowing: Boolean): UserEntity
    suspend fun updateMutingStatus(targetUserId: UserId, isMuting: Boolean): UserEntity
    suspend fun updateBlockingStatus(targetUserId: UserId, isBlocking: Boolean): UserEntity
    suspend fun updateWantRetweetStatus(targetUserId: UserId, wantRetweets: Boolean): Relationship
    suspend fun updateRelationship(relationship: Relationship)

    interface Local : RelationDataSource
    interface Remote : RelationDataSource
}
