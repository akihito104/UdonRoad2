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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.freshdigitable.udonroad2.model.ActivityScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Provider

class ViewModelProviderFactory @Inject constructor(
    private val providers: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val provider: Provider<out ViewModel> = providers[modelClass]
            ?: find(modelClass)
            ?: throw IllegalStateException("unregistered class: $modelClass. all classes:(${providers.keys})")
        @Suppress("UNCHECKED_CAST")
        return provider.get() as T
    }

    private fun <T : ViewModel> find(modelClass: Class<T>): Provider<out ViewModel>? {
        return providers.entries.firstOrNull { (k, _) ->
            modelClass.isAssignableFrom(k)
        }?.value
    }

}

@Module
interface ViewModelModule {
    @Binds
    fun bindViewModelFactory(factory: ViewModelProviderFactory): ViewModelProvider.Factory

    companion object {
        @ActivityScope
        @Provides
        fun provideViewModelProvider(
            viewModelStoreOwner: ViewModelStoreOwner,
            viewModelFactory: ViewModelProvider.Factory
        ): ViewModelProvider = ViewModelProvider(viewModelStoreOwner, viewModelFactory)
    }
}
