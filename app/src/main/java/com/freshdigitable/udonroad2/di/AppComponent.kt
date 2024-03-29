/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

import android.app.Application
import com.freshdigitable.udonroad2.AppApplication
import com.freshdigitable.udonroad2.AppFileProviderImpl
import com.freshdigitable.udonroad2.AppSetup
import com.freshdigitable.udonroad2.AppSetupModule
import com.freshdigitable.udonroad2.DbCleaner
import com.freshdigitable.udonroad2.data.impl.di.RepositoryModule
import com.freshdigitable.udonroad2.data.local.di.DatabaseModule
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import com.freshdigitable.udonroad2.input.di.MediaChooserModule
import com.freshdigitable.udonroad2.model.app.AppFileProvider
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        ActivityBuilders::class,
        ExecutorModule::class,
        RepositoryModule::class,
        DatabaseModule::class,
        TwitterModule::class,
        AppSetupModule::class,
        MediaChooserModule::class,
        AppFileProviderModule::class
    ]
)
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun sharedPreferencesName(name: String): Builder

        fun build(): AppComponent
    }

    val setup: AppSetup
    val dbCleaner: DbCleaner

    fun inject(instance: AppApplication)
}

@Module
interface AppFileProviderModule {
    companion object {
        @Singleton
        @Provides
        fun provideAppFileProvider(): AppFileProvider = AppFileProviderImpl()
    }
}
