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

package com.freshdigitable.udonroad2.timeline.di

import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.TimelineNavigationDelegate
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentEventDelegate
import com.freshdigitable.udonroad2.timeline.viewmodel.CustomTimelineListViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.TimelineViewModel
import com.freshdigitable.udonroad2.timeline.viewmodel.UserListViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
interface TimelineListItemFragmentEventDelegateModule {
    @Binds
    @IntoMap
    @ViewModelKey(TimelineViewModel::class)
    fun bindTimelineEventDelegate(
        eventDelegate: TimelineNavigationDelegate
    ): ListItemFragmentEventDelegate

    @Binds
    @IntoMap
    @ViewModelKey(CustomTimelineListViewModel::class)
    fun bindCustomTimelineListEventDelegate(
        eventDelegate: TimelineNavigationDelegate
    ): ListItemFragmentEventDelegate

    companion object {
        @Provides
        @IntoMap
        @ViewModelKey(UserListViewModel::class)
        fun provideUserListEventDelegate(): ListItemFragmentEventDelegate {
            return object : ListItemFragmentEventDelegate {
                override fun dispatchNavHostNavigate(event: NavigationEvent) {}
                override fun dispatchFeedbackMessage(message: FeedbackMessage) {}
            }
        }
    }
}
