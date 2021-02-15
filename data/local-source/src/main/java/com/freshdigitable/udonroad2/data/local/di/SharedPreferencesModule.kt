/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.local.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal interface SharedPreferencesModule {
    companion object {
        const val NAMED_SETTING_TWITTER = "setting_twitter"
        const val NAMED_SETTING_APP = "setting_app"

        @Provides
        @Singleton
        @Named(NAMED_SETTING_TWITTER)
        fun Application.provideSharedPreferences(name: String): SharedPreferences =
            getSharedPreferences(name, Context.MODE_PRIVATE)

        @Provides
        @Singleton
        @Named(NAMED_SETTING_APP)
        fun Application.providePreference(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
    }
}
