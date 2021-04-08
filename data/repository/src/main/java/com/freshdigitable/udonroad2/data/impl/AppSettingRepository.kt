/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingRepository @Inject constructor(
    private val prefs: AppSettingDataSource.Local,
    private val oAuthApiClient: AppSettingDataSource.Remote,
    executor: AppExecutor,
) : AppSettingDataSource by prefs {
    override val currentUserIdSource: Flow<UserId> =
        prefs.currentUserIdSource.shareIn(executor, SharingStarted.Lazily, 1)

    override suspend fun updateCurrentUser(accessToken: AccessTokenEntity) {
        prefs.updateCurrentUser(accessToken)
        oAuthApiClient.updateCurrentUser(accessToken)
    }
}
