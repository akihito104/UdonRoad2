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

package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.map
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.StateHolder
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import javax.inject.Inject

class TimelineViewState @Inject constructor(
    owner: ListOwner<*>,
    actions: TimelineActions,
    selectedItemRepository: SelectedItemRepository
) {
    private val _selectedItemId: AppViewState<StateHolder<SelectedItemId>> = AppAction.merge(
        AppAction.just(owner).map {
            StateHolder(selectedItemRepository.find(it))
        },
        actions.selectItem.map {
            if (owner == it.owner) {
                selectedItemRepository.put(it.selectedItemId)
                StateHolder(selectedItemRepository.find(it.owner))
            } else {
                throw IllegalStateException()
            }
        },
        actions.unselectItem.map {
            if (owner == it.owner) {
                selectedItemRepository.remove(it.owner)
                StateHolder(null)
            } else {
                throw IllegalStateException()
            }
        },
        actions.toggleItem.map {
            if (owner == it.owner) {
                val current = selectedItemRepository.find(it.item.owner)
                when (it.item) {
                    current -> selectedItemRepository.remove(it.item.owner)
                    else -> selectedItemRepository.put(it.item)
                }
                StateHolder(selectedItemRepository.find(it.owner))
            } else {
                throw IllegalStateException()
            }
        }
    ).toViewState()

    val selectedItemId: AppViewState<SelectedItemId?> = _selectedItemId.map { it.value }
}
