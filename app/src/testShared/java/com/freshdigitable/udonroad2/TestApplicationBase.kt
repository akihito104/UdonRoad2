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

import androidx.test.core.app.ApplicationProvider
import com.freshdigitable.udonroad2.di.AppComponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

abstract class TestApplicationBase : AppApplication() {

    override fun createComponent(): AppComponent {
        return DaggerTestAppComponent.builder()
            .application(ApplicationProvider.getApplicationContext())
            .build()
    }
}

@Module
object MockSetupModule {
    @Provides
    @Singleton
    fun provideMockSetup(): AppSetup = {}
}