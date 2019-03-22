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

package com.freshdigitable.udonroad2.timeline

import android.util.Log
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.repository.HomeTimelineRepository
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides

class TimelineViewModel(
        private val homeRepository: HomeTimelineRepository
) : ViewModel() {

    val timeline: LiveData<PagedList<TweetListItem>> by lazy {
        homeRepository.timeline
    }

    val loading: LiveData<Boolean> by lazy {
        homeRepository.loading
    }

    fun onRefresh() {
        homeRepository.loadAtFront()
    }

    override fun onCleared() {
        super.onCleared()
        homeRepository.clear()
    }

    fun onFabMenuSelected(item: MenuItem) {
        Log.d("TimelineViewModel", "onFabSelected: $item")
    }
}

@Module
object TimelineViewModelModule {
    @Provides
    @JvmStatic
    fun provideMainViewModel(repositories: RepositoryComponent.Builder): TimelineViewModel {
        return TimelineViewModel(repositories.build().homeTimelineRepository())
    }
}
