/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.freshdigitable.udonroad2.model.app.ClassKeyMap
import com.freshdigitable.udonroad2.model.app.valueByAssignableClass
import com.freshdigitable.udonroad2.oauth.di.OauthViewModelModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Inject
import javax.inject.Provider

class ViewModelProviderFactory @Inject constructor(
    private val providers: ClassKeyMap<ViewModel, Provider<ViewModel>>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return providers.valueByAssignableClass(modelClass).get() as T
    }
}

@Module
interface ViewModelModule {
    @Binds
    fun bindViewModelFactory(factory: ViewModelProviderFactory): ViewModelProvider.Factory

    companion object {
        @Provides
        fun provideViewModelProvider(
            viewModelStoreOwner: ViewModelStoreOwner,
            viewModelFactory: ViewModelProvider.Factory
        ): ViewModelProvider = ViewModelProvider(viewModelStoreOwner, viewModelFactory)
    }
}

class AppSavedStateViewModelFactory @Inject constructor(
    private val savedStateViewModelComponent: SavedStateViewModelComponent.Factory,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(savedStateRegistryOwner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T = savedStateViewModelComponent.create(handle)
        .viewModelProviderFactory()
        .create(modelClass)
}

@Module(subcomponents = [SavedStateViewModelComponent::class])
interface SavedStateViewModelModule {
    @Binds
    fun bindAbstractSavedStateViewModelProviderFactory(
        factory: AppSavedStateViewModelFactory
    ): AbstractSavedStateViewModelFactory
}

@Subcomponent(modules = [OauthViewModelModule::class])
interface SavedStateViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance handle: SavedStateHandle): SavedStateViewModelComponent
    }

    fun viewModelProviderFactory(): ViewModelProviderFactory
}
