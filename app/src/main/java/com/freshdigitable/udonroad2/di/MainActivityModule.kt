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

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.freshdigitable.udonroad2.main.MainActivity
import com.freshdigitable.udonroad2.main.MainActivityNavigationDelegate
import com.freshdigitable.udonroad2.main.MainActivityViewStates
import com.freshdigitable.udonroad2.main.MainViewModel
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module(
    includes = [
        ListItemFragmentModule::class,
        TweetDetailFragmentModule::class
    ]
)
interface MainActivityModule {
    @Binds
    fun bindViewModelStoreOwner(activity: MainActivity): ViewModelStoreOwner

    @Binds
    fun bindAppCompatActivity(activity: MainActivity): AppCompatActivity

    @Binds
    fun bindActivityEventDelegate(navDelegate: MainActivityNavigationDelegate): ActivityEventDelegate

    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(MainViewModel::class)
        fun provideMainViewModel(
            navigator: EventDispatcher,
            viewState: MainActivityViewStates
        ): ViewModel = MainViewModel(navigator, viewState)
    }
}
