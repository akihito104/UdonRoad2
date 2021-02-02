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

package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import com.freshdigitable.udonroad2.data.local.requireCurrentUserId
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ListOwnerRepository @Inject constructor(
    private val listDao: ListDao,
    private val prefsDataSource: AppSettingDataSource.Local,
) : ListOwnerGenerator {
    companion object {
        private val oauthListId: ListId = ListId(-1)
    }

    override suspend fun <Q : QueryType> generate(type: Q): ListOwner<Q> {
        val id = when (type) {
            QueryType.Oauth -> oauthListId
            else -> {
                listDao.addList(prefsDataSource.requireCurrentUserId())
            }
        }
        return ListOwner(id, type)
    }
}

fun ListOwnerGenerator.Companion.create(
    idGenerator: AtomicInteger = AtomicInteger(0)
): ListOwnerGenerator = object : ListOwnerGenerator {
    override suspend fun <Q : QueryType> generate(type: Q): ListOwner<Q> {
        return ListOwner(idGenerator.getAndIncrement(), type)
    }
}
