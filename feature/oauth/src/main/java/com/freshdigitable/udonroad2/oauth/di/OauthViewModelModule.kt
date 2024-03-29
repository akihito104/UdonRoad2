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

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.PagingSource
import androidx.savedstate.SavedStateRegistryOwner
import com.freshdigitable.udonroad2.data.OAuthTokenDataSource
import com.freshdigitable.udonroad2.data.impl.di.ListRepositoryComponentModule
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.IntoSavedStateFactory
import com.freshdigitable.udonroad2.model.app.di.QueryTypeKey
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectDelegate
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.oauth.LoginUseCase
import com.freshdigitable.udonroad2.oauth.OauthAction
import com.freshdigitable.udonroad2.oauth.OauthDataSource
import com.freshdigitable.udonroad2.oauth.OauthItem
import com.freshdigitable.udonroad2.oauth.OauthNavigationDelegate
import com.freshdigitable.udonroad2.oauth.OauthSavedStates
import com.freshdigitable.udonroad2.oauth.OauthViewModel
import com.freshdigitable.udonroad2.oauth.OauthViewModelSource
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEffectDelegate
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.reflect.KClass

@Module(includes = [ListRepositoryComponentModule::class])
interface OauthViewModelModule {
    @Binds
    fun bindSavedStateRegistryOwner(fragment: ListItemFragment): SavedStateRegistryOwner

    companion object {
        @Provides
        internal fun provideOauthAction(dispatcher: EventDispatcher): OauthAction {
            return OauthAction(dispatcher)
        }

        @Provides
        @IntoMap
        @ViewModelKey(OauthViewModel::class)
        fun provideOauthNavigationDelegate(
            fragment: ListItemFragment,
            activityEffectDelegate: ActivityEffectDelegate,
        ): ListItemFragmentEffectDelegate {
            return OauthNavigationDelegate(fragment, activityEffectDelegate)
        }

        @Provides
        fun provideOauthDataSource(context: Application): PagingSource<Int, OauthItem> {
            return OauthDataSource(context)
        }

        @Provides
        fun provideOauthSavedStates(handle: SavedStateHandle): OauthSavedStates =
            OauthSavedStates(handle)

        @Provides
        internal fun provideOauthViewStates(
            actions: OauthAction,
            login: LoginUseCase,
            dataSource: PagingSource<Int, OauthItem>,
            repository: OAuthTokenDataSource,
            listOwnerGenerator: ListOwnerGenerator,
            savedState: OauthSavedStates,
        ): OauthViewModelSource {
            return OauthViewModelSource(
                actions,
                login,
                dataSource,
                repository,
                listOwnerGenerator,
                savedState
            )
        }

        @Provides
        @IntoMap
        @ViewModelKey(OauthViewModel::class)
        @IntoSavedStateFactory
        internal fun provideOauthViewModel(viewModelSource: OauthViewModelSource): ViewModel =
            OauthViewModel(viewModelSource)

        @Provides
        @IntoMap
        @QueryTypeKey(QueryType.Oauth::class)
        fun provideOauthViewModelKClass(): KClass<out ViewModel> = OauthViewModel::class
    }
}
