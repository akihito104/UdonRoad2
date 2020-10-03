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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelScope
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewStates
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap

@Module
interface TweetDetailViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(TweetDetailViewModel::class)
        @ViewModelScope
        fun provideTweetDetailViewModel(
            eventDispatcher: EventDispatcher,
            viewStates: TweetDetailViewStates,
        ): ViewModel {
            return TweetDetailViewModel(eventDispatcher, viewStates)
        }
    }
}

@ViewModelScope
@Subcomponent(modules = [TweetDetailViewModelModule::class])
interface TweetDetailViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance tweetId: TweetId): TweetDetailViewModelComponent
    }

    val viewModelProvider: ViewModelProvider
}

@Module(subcomponents = [TweetDetailViewModelComponent::class])
interface TweetDetailViewModelComponentModule
