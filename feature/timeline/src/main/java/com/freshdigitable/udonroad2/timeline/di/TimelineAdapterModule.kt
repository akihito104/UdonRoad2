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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.paging.PagingDataAdapter
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.timeline.listadapter.CustomTimelineListAdapter
import com.freshdigitable.udonroad2.timeline.listadapter.TimelineAdapter
import com.freshdigitable.udonroad2.timeline.listadapter.UserListAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.CustomTimelineListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module(
    includes = [
        TimelineAdapterModule::class,
        UserListAdapterModule::class,
        CustomTimelineListAdapterModule::class
    ]
)
interface TimelineAdapterModules

@Module
internal object TimelineAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun provideTimelineAdapter(
        viewModel: ViewModel,
        lifecycleOwner: LifecycleOwner
    ): PagingDataAdapter<out Any, *> {
        val vm = viewModel as TimelineViewModel
        return TimelineAdapter(vm, vm, vm, lifecycleOwner)
    }
}

@Module
internal object UserListAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun provideTimelineAdapter(viewModel: ViewModel): PagingDataAdapter<out Any, *> {
        val vm = viewModel as UserListViewModel
        return UserListAdapter(vm, vm)
    }
}

@Module
internal object CustomTimelineListAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(CustomTimelineListViewModel::class)
    fun provideTimelineAdapter(viewModel: ViewModel): PagingDataAdapter<out Any, *> {
        val vm = viewModel as CustomTimelineListViewModel
        return CustomTimelineListAdapter(vm)
    }
}
