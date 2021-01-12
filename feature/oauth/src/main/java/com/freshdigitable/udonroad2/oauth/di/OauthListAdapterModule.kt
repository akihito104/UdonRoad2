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

package com.freshdigitable.udonroad2.oauth.di

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.paging.PagingDataAdapter
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.oauth.OauthListAdapter
import com.freshdigitable.udonroad2.oauth.OauthViewModel
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
object OauthListAdapterModule {
    @Provides
    @IntoMap
    @ViewModelKey(OauthViewModel::class)
    fun provideOauthAdapter(
        viewModel: ViewModel,
        lifecycleOwner: LifecycleOwner
    ): PagingDataAdapter<out Any, *> {
        return OauthListAdapter(viewModel as OauthViewModel, lifecycleOwner)
    }
}
