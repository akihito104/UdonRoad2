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

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedItemRepository @Inject constructor() {
    private val selectedItemsSource =
        MutableStateFlow<Map<ListOwner<*>, SelectedItemId>>(emptyMap())

    fun put(itemId: SelectedItemId) {
        val newItems = selectedItemsSource.value.plus(itemId.owner to itemId)
        selectedItemsSource.value = newItems
    }

    fun remove(owner: ListOwner<*>) {
        val newItems = selectedItemsSource.value.minus(owner)
        selectedItemsSource.value = newItems
    }

    fun find(owner: ListOwner<*>): SelectedItemId? = selectedItemsSource.value[owner]

    fun getSource(owner: ListOwner<*>): Flow<SelectedItemId?> =
        selectedItemsSource.map { it[owner] }.distinctUntilChanged()
}
