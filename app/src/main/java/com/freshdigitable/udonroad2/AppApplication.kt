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

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.freshdigitable.udonroad2.di.AppComponent
import com.freshdigitable.udonroad2.di.DaggerAppComponent
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.oauth.LoginUseCase
import com.freshdigitable.udonroad2.oauth.LoginUseCase.Companion.invokeOnLaunchApp
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

open class AppApplication : HasAndroidInjector, Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        Timber.plant(Timber.DebugTree())
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        val component = createComponent()
        component.setup()
        component.inject(this)
    }

    protected open fun createComponent(): AppComponent {
        return DaggerAppComponent.builder()
            .application(this)
            .sharedPreferencesName("udonroad_prefs")
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
        login: LoginUseCase,
        executor: AppExecutor,
    ): AppSetup = {
        executor.launch {
            login.invokeOnLaunchApp()
        }
    }
}
