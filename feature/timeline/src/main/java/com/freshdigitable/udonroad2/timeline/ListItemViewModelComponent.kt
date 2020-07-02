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

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.savedstate.SavedStateRegistryOwner

interface ListItemViewModelComponent {

    interface Builder {
        fun owner(owner: ListOwner<*>): Builder
        fun savedStateRegistryOwner(owner: SavedStateRegistryOwner): Builder
        fun firstArgs(bundle: Bundle?): Builder
        fun build(): ListItemViewModelComponent
    }

    fun savedStateViewModelProviderFactory(): AbstractSavedStateViewModelFactory
}
