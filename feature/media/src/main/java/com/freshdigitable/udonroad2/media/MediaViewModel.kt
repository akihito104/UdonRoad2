/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.repository.RepositoryComponent
import com.freshdigitable.udonroad2.data.repository.TweetRepository
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.TweetListItem
import dagger.Module
import dagger.Provides

class MediaViewModel(
    tweetRepository: TweetRepository
) : ViewModel() {
    private val id: MutableLiveData<Long?> = MutableLiveData()
    internal val tweet: LiveData<TweetListItem?> = id.switchMap {
        if (it != null) tweetRepository.getTweetItem(it)
        else MutableLiveData()
    }
    internal val mediaItems: LiveData<List<MediaItem>> = tweet.map {
        it?.body?.mediaItems ?: listOf()
    }

    internal fun setId(id: Long) {
        this.id.value = id
    }
}

@Module
object MediaViewModelModule {
    @Provides
    @JvmStatic
    fun provideMediaViewModel(repositoryComponent: RepositoryComponent.Builder): MediaViewModel {
        return MediaViewModel(repositoryComponent.build().tweetRepository())
    }
}
