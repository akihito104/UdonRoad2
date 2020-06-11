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
import com.freshdigitable.udonroad2.data.db.DatabaseModule
import com.freshdigitable.udonroad2.data.impl.RepositoryComponent
import com.freshdigitable.udonroad2.data.impl.RepositoryModule
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import dagger.BindsInstance
import dagger.Component
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
        TwitterModule::class
    ]
)
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }

    fun repositoryModule(): RepositoryComponent.Builder

    fun inject(instance: AppApplication)
}
