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

package com.freshdigitable.udonroad2.settings

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import javax.inject.Inject

class AppSettingFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory

    private val viewModel: AppSettingViewModel by viewModels(
        factoryProducer = { viewModelProviderFactory }
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.user_settings, rootKey)
    }
}

class AppSettingViewModel @Inject constructor(
    private val repository: AppSettingRepository
) : ViewModel()

@Module
interface AppSettingFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [AppSettingModule::class])
    fun contributeAppSettingFragment(): AppSettingFragment
}

@Module
internal interface AppSettingModule {
    @Binds
    @IntoMap
    @ViewModelKey(AppSettingViewModel::class)
    fun bindAppSettingViewModel(viewModel: AppSettingViewModel): ViewModel
}
