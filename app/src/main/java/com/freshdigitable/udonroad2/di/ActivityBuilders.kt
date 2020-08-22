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
import android.content.Context
import android.content.SharedPreferences
import com.freshdigitable.udonroad2.main.MainActivity
import com.freshdigitable.udonroad2.media.MediaActivity
import com.freshdigitable.udonroad2.media.MediaActivityModule
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.user.UserActivity
import com.freshdigitable.udonroad2.user.UserActivityModule
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Singleton

@Module
interface ActivityBuilders {
    @ActivityScope
    @ContributesAndroidInjector(
        modules = [
            MainActivityModule::class,
            ViewModelModule::class
        ]
    )
    fun contributesMainActivity(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(
        modules = [
            UserActivityModule::class,
            ViewModelModule::class
        ]
    )
    fun contributeUserActivity(): UserActivity

    @ActivityScope
    @ContributesAndroidInjector(
        modules = [
            MediaActivityModule::class,
            ViewModelModule::class
        ]
    )
    fun contributeMediaActivity(): MediaActivity
}

@Module
object SharedPreferencesModule {
    @Provides
    @Singleton
    fun Application.provideSharedPreferences(): SharedPreferences {
        return getSharedPreferences("udonroad_prefs", Context.MODE_PRIVATE)
    }
}
