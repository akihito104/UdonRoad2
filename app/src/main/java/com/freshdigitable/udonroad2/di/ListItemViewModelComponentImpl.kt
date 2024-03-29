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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.paging.PagingDataAdapter
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.ClassKeyMap
import com.freshdigitable.udonroad2.model.app.di.IntoFactory
import com.freshdigitable.udonroad2.model.app.di.ViewModelScope
import com.freshdigitable.udonroad2.model.app.valueByAssignableClassObject
import com.freshdigitable.udonroad2.oauth.di.OauthListAdapterModule
import com.freshdigitable.udonroad2.oauth.di.OauthViewModelModule
import com.freshdigitable.udonroad2.timeline.di.ListItemAdapterComponent
import com.freshdigitable.udonroad2.timeline.di.ListItemFragmentEffectDelegateComponent
import com.freshdigitable.udonroad2.timeline.di.ListItemViewModelComponent
import com.freshdigitable.udonroad2.timeline.di.TimelineAdapterModules
import com.freshdigitable.udonroad2.timeline.di.TimelineListItemFragmentEffectDelegateModule
import com.freshdigitable.udonroad2.timeline.di.TimelineViewModelModules
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEffectDelegate
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named
import javax.inject.Provider
import kotlin.reflect.KClass

@Module(subcomponents = [ListItemViewModelComponentImpl::class])
interface ListItemViewModelModule {
    companion object {
        @Provides
        fun provideListItemViewModelComponentBuilder(
            builder: ListItemViewModelComponentImpl.Builder,
        ): ListItemViewModelComponent.Builder = builder
    }
}

private const val COMPOSED_VIEW_MODEL_PROVIDER = "ComposedViewModelProvider"

@ViewModelScope
@Subcomponent(modules = [ViewModelClassProvider::class])
interface ListItemViewModelComponentImpl : ListItemViewModelComponent {

    @Subcomponent.Builder
    interface Builder : ListItemViewModelComponent.Builder {
        @BindsInstance
        override fun owner(owner: ListOwner<*>): Builder

        @BindsInstance
        override fun firstArgs(bundle: Bundle?): Builder

        @BindsInstance
        override fun viewModelStoreOwner(storeOwner: ViewModelStoreOwner): Builder

        override fun build(): ListItemViewModelComponentImpl
    }

    @get:Named(COMPOSED_VIEW_MODEL_PROVIDER)
    override val viewModelProvider: ViewModelProvider
}

@Module(
    includes = [
        TimelineViewModelModules::class,
        OauthViewModelModule::class,
        SavedStateViewModelModule::class,
    ]
)
object ViewModelClassProvider {
    @Provides
    fun ClassKeyMap<QueryType, KClass<out ViewModel>>.provideViewModelClass(
        owner: ListOwner<*>,
    ): Class<out ViewModel> = this.valueByAssignableClassObject(owner.query).java

    @Provides
    @Named(COMPOSED_VIEW_MODEL_PROVIDER)
    fun provideViewModelProvider(
        owner: ViewModelStoreOwner,
        @IntoFactory providers: ClassKeyMap<ViewModel, Provider<ViewModel>>,
        savedStateFactory: AppSavedStateViewModelFactory,
    ): ViewModelProvider {
        val f = ViewModelProviderFactory(providers)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    providers.keys.contains(modelClass) -> f.create(modelClass)
                    else -> savedStateFactory.create(modelClass)
                }
            }
        }
        return ViewModelProvider(owner, factory)
    }
}

@Module(subcomponents = [ListItemAdapterComponentImpl::class])
interface ListItemAdapterModule {
    companion object {
        @Provides
        fun provideListItemAdapterComponentFactory(
            factory: ListItemAdapterComponentImpl.Factory,
        ): ListItemAdapterComponent.Factory = factory
    }
}

@Subcomponent(
    modules = [
        TimelineAdapterModules::class,
        OauthListAdapterModule::class,
        ListItemAdapterProvider::class
    ]
)
interface ListItemAdapterComponentImpl : ListItemAdapterComponent {
    @Subcomponent.Factory
    interface Factory : ListItemAdapterComponent.Factory {
        override fun create(
            @BindsInstance viewModel: ViewModel,
            @BindsInstance lifecycleOwner: LifecycleOwner,
        ): ListItemAdapterComponentImpl
    }
}

@Module
object ListItemAdapterProvider {
    @Provides
    fun ClassKeyMap<ViewModel, Provider<PagingDataAdapter<out Any, *>>>.provideItemAdapter(
        viewModel: ViewModel,
    ): PagingDataAdapter<out Any, *> = this.valueByAssignableClassObject(viewModel).get()
}

@Subcomponent(
    modules = [
        TimelineListItemFragmentEffectDelegateModule::class,
        OauthViewModelModule::class,
        ListItemFragmentEventDelegateProvider::class
    ]
)
interface ListItemFragmentEffectDelegateComponentImpl : ListItemFragmentEffectDelegateComponent {
    @Subcomponent.Factory
    interface Factory : ListItemFragmentEffectDelegateComponent.Factory {
        override fun create(
            @BindsInstance viewModel: ViewModel,
        ): ListItemFragmentEffectDelegateComponentImpl
    }
}

@Module
object ListItemFragmentEventDelegateProvider {
    @Provides
    fun ClassKeyMap<ViewModel, Provider<ListItemFragmentEffectDelegate>>.provideEventDelegate(
        viewModel: ViewModel,
    ): ListItemFragmentEffectDelegate = this.valueByAssignableClassObject(viewModel).get()
}

@Module(subcomponents = [ListItemFragmentEffectDelegateComponentImpl::class])
interface ListItemFragmentEventDelegateModule {
    companion object {
        @Provides
        fun provideFactory(
            factory: ListItemFragmentEffectDelegateComponentImpl.Factory,
        ): ListItemFragmentEffectDelegateComponent.Factory = factory
    }
}
