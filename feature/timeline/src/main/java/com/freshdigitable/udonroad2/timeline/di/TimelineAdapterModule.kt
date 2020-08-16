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
import androidx.paging.PagedListAdapter
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.timeline.listadapter.MemberListListAdapter
import com.freshdigitable.udonroad2.timeline.listadapter.TimelineAdapter
import com.freshdigitable.udonroad2.timeline.listadapter.UserListAdapter
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
object TimelineAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun provideTimelineAdapter(
        viewModel: ViewModel,
        lifecycleOwner: LifecycleOwner
    ): PagedListAdapter<out Any, *> {
        val vm = viewModel as TimelineViewModel
        return TimelineAdapter(vm, vm, lifecycleOwner)
    }
}

@Module
object UserListAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    fun provideTimelineAdapter(viewModel: ViewModel): PagedListAdapter<out Any, *> {
        val vm = viewModel as UserListViewModel
        return UserListAdapter(vm)
    }
}

@Module
object MemberListListAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(MemberListListViewModel::class)
    fun provideTimelineAdapter(viewModel: ViewModel): PagedListAdapter<out Any, *> {
        val vm = viewModel as MemberListListViewModel
        return MemberListListAdapter(vm)
    }
}
