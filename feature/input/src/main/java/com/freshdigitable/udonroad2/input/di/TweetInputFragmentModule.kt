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

package com.freshdigitable.udonroad2.input.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.input.CameraAppActions
import com.freshdigitable.udonroad2.input.CameraAppEventListener
import com.freshdigitable.udonroad2.input.MediaChooserBroadcastReceiver
import com.freshdigitable.udonroad2.input.TweetInputFragment
import com.freshdigitable.udonroad2.input.TweetInputViewModel
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
internal interface TweetInputViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(TweetInputViewModel::class)
    fun bindTweetInputViewModel(viewModel: TweetInputViewModel): ViewModel
}

@Subcomponent(modules = [TweetInputViewModelModule::class])
interface TweetInputViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance collapsible: Boolean): TweetInputViewModelComponent
    }

    val viewModelProviderFactory: ViewModelProvider.Factory
}

@Module(subcomponents = [TweetInputViewModelComponent::class])
interface TweetInputViewModelComponentModule

@Module
interface TweetInputFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributeTweetInputFragment(): TweetInputFragment
}

@Module(includes = [MediaChooserModuleImpl::class])
interface MediaChooserModule {
    @ContributesAndroidInjector
    fun contributeMediaChooserBroadcastReceiver(): MediaChooserBroadcastReceiver
}

@Module
internal interface MediaChooserModuleImpl {
    @Binds
    fun bindCameraAppEventListener(eventListener: CameraAppActions): CameraAppEventListener

    companion object {
        @Provides
        @Singleton
        fun provideCameraAppActions(): CameraAppActions = CameraAppActions()
    }
}
