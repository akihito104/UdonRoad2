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

package com.freshdigitable.udonroad2

import android.app.Application
import com.freshdigitable.udonroad2.di.AppComponent
import com.freshdigitable.udonroad2.di.DaggerAppComponent
import com.jakewharton.threetenabp.AndroidThreeTen
import com.squareup.leakcanary.LeakCanary
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject
import javax.inject.Singleton

open class AppApplication : HasAndroidInjector, Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)

        val component = createComponent()
        component.setup()
        component.inject(this)
    }

    protected open fun createComponent(): AppComponent {
        return DaggerAppComponent.builder()
            .application(this)
            .build()
    }

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}

typealias AppSetup = () -> Unit

@Module
object AppSetupModule {
    @Provides
    @Singleton
    fun provideSetup(
        app: Application
    ): AppSetup = {
        app.setupLeakCanary()
    }

    private fun Application.setupLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)
    }
}
