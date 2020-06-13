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

package com.freshdigitable.udonroad2.di

import com.freshdigitable.udonroad2.data.impl.ListRepositoryModule
import com.freshdigitable.udonroad2.oauth.OauthViewModelModule
import com.freshdigitable.udonroad2.timeline.ListItemViewModelComponent
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModelModule
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModelModule
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModelModule
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@Module(subcomponents = [ListItemViewModelComponentImpl::class])
interface ListItemViewModelModule {
    companion object {
        @Provides
        fun provideListItemViewModelComponentBuilder(
            builder: ListItemViewModelComponentImpl.Builder
        ): ListItemViewModelComponent.Builder = builder
    }
}

@Subcomponent(
    modules = [
        TimelineViewModelModule::class,
        MemberListListViewModelModule::class,
        UserListViewModelModule::class,
        ListRepositoryModule::class,
        OauthViewModelModule::class
    ]
)
interface ListItemViewModelComponentImpl : ListItemViewModelComponent {

    @Subcomponent.Builder
    interface Builder : ListItemViewModelComponent.Builder {
        @BindsInstance
        override fun owner(owner: ListOwner<*>): Builder

        override fun build(): ListItemViewModelComponentImpl
    }
}
