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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectDelegate
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.user.UserActivity
import com.freshdigitable.udonroad2.user.UserActivityNavigationDelegate
import com.freshdigitable.udonroad2.user.UserViewModel
import com.freshdigitable.udonroad2.user.UserViewModelSource
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.IntoMap

@Module(
    includes = [
        ListItemFragmentModule::class,
        UserViewModelComponentModule::class
    ]
)
interface UserActivityModule {
    @Binds
    fun bindActivityEventDelegate(
        eventDelegate: UserActivityNavigationDelegate,
    ): ActivityEffectDelegate

    companion object {
        @Provides
        fun provideActivityEventDelegate(activity: UserActivity): UserActivityNavigationDelegate {
            return UserActivityNavigationDelegate(activity)
        }
    }
}

@Module
interface UserViewModelModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(UserViewModel::class)
        fun provideUserViewModel(
            eventDispatcher: EventDispatcher,
            viewState: UserViewModelSource,
        ): ViewModel = UserViewModel(eventDispatcher, viewState)
    }
}

@Subcomponent(modules = [UserViewModelModule::class])
interface UserViewModelComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance user: TweetUserItem): UserViewModelComponent
    }

    val viewModelProviderFactory: ViewModelProvider.Factory
}

@Module(subcomponents = [UserViewModelComponent::class])
interface UserViewModelComponentModule
