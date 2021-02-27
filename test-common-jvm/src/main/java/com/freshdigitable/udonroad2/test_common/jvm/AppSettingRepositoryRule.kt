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

package com.freshdigitable.udonroad2.test_common.jvm

import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import org.junit.rules.TestRule

class AppSettingRepositoryRule(
    internal val rule: MockVerified<AppSettingRepository> = MockVerified.create()
) : TestRule by rule {
    val mock: AppSettingRepository = rule.mock

    fun setupCurrentUserId(userId: Long?) = rule.run {
        setupResponseWithVerify({ mock.currentUserId }, userId?.let { UserId(it) })
    }

    val currentUserIdSource: Channel<UserId> = Channel()
    fun setupCurrentUserIdSource(coroutineScope: CoroutineScope, userId: Long? = null) = rule.run {
        setupResponseWithVerify(
            { mock.currentUserIdSource },
            currentUserIdSource.receiveAsFlow()
                .onStart { userId?.let { emit(UserId(it)) } }
                .shareIn(coroutineScope, SharingStarted.Lazily, 1)
        )
    }

    val registeredUserIdsSource = Channel<Set<UserId>>()
    fun setupRegisteredUserIdsSource(userIds: Set<UserId> = emptySet()) = rule.run {
        setupResponseWithVerify(
            { mock.registeredUserIdsSource },
            registeredUserIdsSource.receiveAsFlow().onStart { emit(userIds) }
        )
    }

    fun setupUpdateCurrentUserId(token: AccessTokenEntity) = rule.run {
        coSetupResponseWithVerify({ mock.updateCurrentUser(token) }, Unit)
    }

    fun setupIsPossiblySensitiveHidden() = rule.run {
        setupResponseWithVerify({ mock.isPossiblySensitiveHidden }, flowOf(true))
    }
}
