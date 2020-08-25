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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectedItemRepository @Inject constructor() {
    private val selectedItems: MutableMap<ListOwner<*>, SelectedItemId> = mutableMapOf()
    private val selectedItemsSource =
        MutableLiveData<Map<ListOwner<*>, SelectedItemId>>(selectedItems)

    fun put(itemId: SelectedItemId) {
        selectedItems[itemId.owner] = itemId
        selectedItemsSource.value = selectedItems
    }

    fun remove(owner: ListOwner<*>) {
        selectedItems.remove(owner)
        selectedItemsSource.value = selectedItems
    }

    fun find(owner: ListOwner<*>): SelectedItemId? {
        return selectedItems[owner]
    }

    fun observe(owner: ListOwner<*>): LiveData<SelectedItemId?> {
        return selectedItemsSource.map { it[owner] }
    }
}
