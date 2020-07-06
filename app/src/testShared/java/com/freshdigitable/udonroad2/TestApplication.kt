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

import com.freshdigitable.udonroad2.data.db.DatabaseModule
import com.freshdigitable.udonroad2.data.impl.RepositoryModule
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
import com.freshdigitable.udonroad2.di.ActivityBuilders
import com.freshdigitable.udonroad2.di.AppComponent
import com.freshdigitable.udonroad2.di.ExecutorModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

class TestApplication : AppApplication() {
    override fun createComponent(): AppComponent {
        return DaggerTestAppComponent.builder()
            .setupModule { /* nop */ }
            .application(this)
            .build()
    }
}

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
interface TestAppComponent : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        fun setupModule(setup: AppSetup): Builder
        override fun build(): TestAppComponent
    }
}
