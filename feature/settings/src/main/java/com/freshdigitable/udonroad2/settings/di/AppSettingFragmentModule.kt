/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.settings.di

import androidx.lifecycle.ViewModel
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.settings.AppSettingFragment
import com.freshdigitable.udonroad2.settings.AppSettingViewModel
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
interface AppSettingFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [AppSettingModule::class])
    fun contributeAppSettingFragment(): AppSettingFragment
}

@Module
internal interface AppSettingModule {
    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(AppSettingViewModel::class)
        fun provideAppSettingViewModel(
            repository: AppSettingRepository,
            userRepository: UserDataSource
        ): ViewModel = AppSettingViewModel(repository, userRepository)
    }
}
