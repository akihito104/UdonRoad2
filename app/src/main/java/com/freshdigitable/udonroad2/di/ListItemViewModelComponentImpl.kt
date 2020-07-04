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

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.paging.PagedListAdapter
import androidx.savedstate.SavedStateRegistryOwner
import com.freshdigitable.udonroad2.data.impl.ListRepositoryModule
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.ClassKeyMap
import com.freshdigitable.udonroad2.model.app.valueByAssignableClassObject
import com.freshdigitable.udonroad2.oauth.OauthListAdapterModule
import com.freshdigitable.udonroad2.oauth.OauthViewModelModule
import com.freshdigitable.udonroad2.timeline.ListItemAdapterComponent
import com.freshdigitable.udonroad2.timeline.ListItemViewModelComponent
import com.freshdigitable.udonroad2.timeline.ListOwner
import com.freshdigitable.udonroad2.timeline.listadapter.MemberListListAdapterModule
import com.freshdigitable.udonroad2.timeline.listadapter.TimelineAdapterModule
import com.freshdigitable.udonroad2.timeline.listadapter.UserListAdapterModule
import com.freshdigitable.udonroad2.timeline.viewmodel.MemberListListViewModelModule
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModelModule
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModelModule
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Provider
import kotlin.reflect.KClass

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
        OauthViewModelModule::class,
        ListRepositoryModule::class,
        SavedStateViewModelModule::class,
        ViewModelClassProvider::class
    ]
)
interface ListItemViewModelComponentImpl : ListItemViewModelComponent {

    @Subcomponent.Builder
    interface Builder : ListItemViewModelComponent.Builder {
        @BindsInstance
        override fun owner(owner: ListOwner<*>): Builder

        @BindsInstance
        override fun savedStateRegistryOwner(owner: SavedStateRegistryOwner): Builder

        @BindsInstance
        override fun firstArgs(bundle: Bundle?): Builder

        override fun build(): ListItemViewModelComponentImpl
    }
}

@Module
object ViewModelClassProvider {
    @Provides
    fun ClassKeyMap<QueryType, KClass<out ViewModel>>.provideViewModelClass(
        owner: ListOwner<*>
    ): Class<out ViewModel> = this.valueByAssignableClassObject(owner.query).java
}

@Module(subcomponents = [ListItemAdapterComponentImpl::class])
interface ListItemAdapterModule {
    companion object {
        @Provides
        fun provideListItemAdapterComponentFactory(
            factory: ListItemAdapterComponentImpl.Factory
        ): ListItemAdapterComponent.Factory = factory
    }
}

@Subcomponent(
    modules = [
        TimelineAdapterModule::class,
        UserListAdapterModule::class,
        MemberListListAdapterModule::class,
        OauthListAdapterModule::class,
        ListItemAdapterProvider::class
    ]
)
interface ListItemAdapterComponentImpl : ListItemAdapterComponent {
    @Subcomponent.Factory
    interface Factory : ListItemAdapterComponent.Factory {
        override fun create(
            @BindsInstance viewModel: ViewModel,
            @BindsInstance lifecycleOwner: LifecycleOwner
        ): ListItemAdapterComponentImpl
    }
}

@Module
object ListItemAdapterProvider {
    @Provides
    fun ClassKeyMap<ViewModel, Provider<PagedListAdapter<out Any, *>>>.provideItemAdapter(
        viewModel: ViewModel
    ): PagedListAdapter<out Any, *> = this.valueByAssignableClassObject(viewModel).get()
}
