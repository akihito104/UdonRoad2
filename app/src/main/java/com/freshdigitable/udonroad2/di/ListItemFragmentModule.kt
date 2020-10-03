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

package com.freshdigitable.udonroad2.di

import com.freshdigitable.udonroad2.model.app.di.FragmentScope
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragment
import com.freshdigitable.udonroad2.timeline.viewmodel.TweetDetailViewModelComponentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface ListItemFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(
        modules = [
            ListItemViewModelModule::class,
            ListItemAdapterModule::class
        ]
    )
    fun contributeListItemFragment(): ListItemFragment
}

@Module
interface TweetDetailFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [TweetDetailViewModelComponentModule::class])
    fun contributeTweetDetailFragment(): TweetDetailFragment
}
