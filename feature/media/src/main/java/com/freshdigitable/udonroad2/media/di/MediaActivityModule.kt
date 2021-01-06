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

package com.freshdigitable.udonroad2.media.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.media.MediaViewModel
import com.freshdigitable.udonroad2.media.MovieMediaFragment
import com.freshdigitable.udonroad2.media.PhotoMediaFragment
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module(includes = [MediaViewModelComponentModule::class])
interface MediaActivityModule {
    @FragmentScope
    @ContributesAndroidInjector
    fun contributePhotoMediaFragment(): PhotoMediaFragment

    @FragmentScope
    @ContributesAndroidInjector
    fun contributeMovieMediaFragment(): MovieMediaFragment
}

@Module
interface MediaViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(MediaViewModel::class)
    fun bindMediaViewModel(viewModel: MediaViewModel): ViewModel
}

@Subcomponent(modules = [MediaViewModelModule::class])
interface MediaViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance tweetId: TweetId,
            @BindsInstance firstPosition: Int,
        ): MediaViewModelComponent
    }

    val viewModelProviderFactory: ViewModelProvider.Factory
}

@Module(subcomponents = [MediaViewModelComponent::class])
interface MediaViewModelComponentModule
