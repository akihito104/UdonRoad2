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

package com.freshdigitable.udonroad2.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.DispatcherProvider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import java.util.SortedSet

internal class AppSettingViewModel(
    repository: AppSettingRepository,
    userRepository: UserDataSource,
    dispatcher: DispatcherProvider? = null,
) : ViewModel() {
    private val coroutineContext = dispatcher?.mainContext ?: viewModelScope.coroutineContext
    internal val registeredUserAccount: LiveData<SortedSet<Pair<UserId, String>>> =
        repository.registeredUserIdsSource.onStart { emit(emptySet()) }
            .mapLatest { ids ->
                ids.map { userRepository.getUser(it) }
                    .map { Pair(it.id, "@${it.screenName}") }
                    .toSortedSet { a, b -> a.second.compareTo(b.second) }
            }
            .distinctUntilChanged()
            .asLiveData(coroutineContext)
}
