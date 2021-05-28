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

package com.freshdigitable.udonroad2

import android.app.Application
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.di.RepositoryModule
import com.freshdigitable.udonroad2.data.local.SharedPreferenceDataSource
import com.freshdigitable.udonroad2.data.local.di.DatabaseModule
import com.freshdigitable.udonroad2.di.ActivityBuilders
import com.freshdigitable.udonroad2.di.AppComponent
import com.freshdigitable.udonroad2.di.AppFileProviderModule
import com.freshdigitable.udonroad2.di.ExecutorModule
import com.freshdigitable.udonroad2.input.di.MediaChooserModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import twitter4j.Twitter
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ActivityBuilders::class,
        ExecutorModule::class,
        RepositoryModule::class,
        DatabaseModule::class,
        MockTwitterModule::class,
        MockSetupModule::class,
        MediaChooserModule::class,
        AppFileProviderModule::class,
    ]
)
interface TestAppComponent : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        override fun application(application: Application): Builder

        @BindsInstance
        override fun sharedPreferencesName(name: String): Builder

        override fun build(): TestAppComponent
    }

    val twitter: Twitter
    val sharedPreferencesDao: SharedPreferenceDataSource
    val userDao: UserDataSource.Local
}
