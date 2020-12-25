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

package com.freshdigitable.udonroad2.timeline.di

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEventDelegate

interface ListItemViewModelComponent {

    interface Builder {
        fun owner(owner: ListOwner<*>): Builder
        fun firstArgs(bundle: Bundle?): Builder
        fun viewModelStoreOwner(storeOwner: ViewModelStoreOwner): Builder
        fun build(): ListItemViewModelComponent
    }

    val viewModelClass: Class<out ViewModel>
    val viewModelProvider: ViewModelProvider
}

fun ListItemViewModelComponent.viewModel(key: String): ListItemLoadableViewModel<*, Any> {
    return viewModelProvider.get(key, viewModelClass) as ListItemLoadableViewModel<*, Any>
}

interface ListItemAdapterComponent {
    interface Factory {
        fun create(
            viewModel: ViewModel,
            lifecycleOwner: LifecycleOwner
        ): ListItemAdapterComponent
    }

    val adapter: PagedListAdapter<out Any, *>
}

interface ListItemFragmentEventDelegateComponent {
    interface Factory {
        fun create(
            viewModel: ListItemLoadableViewModel<*, *>
        ): ListItemFragmentEventDelegateComponent
    }

    val eventDelegate: ListItemFragmentEventDelegate
}
