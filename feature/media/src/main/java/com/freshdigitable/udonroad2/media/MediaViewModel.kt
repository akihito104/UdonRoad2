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

import android.app.Application
import android.os.Build
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.ext.merge
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlin.math.min

class MediaViewModel(
    tweetRepository: TweetRepository,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private val SYSTEM_UI_FLAG_FULLSCREEN =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                View.SYSTEM_UI_FLAG_FULLSCREEN
            } else {
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        private val SYSTEM_UI_FLAG_SHOW =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            } else {
                -1
            }
        private val SYSTEM_UI_FLAG_HIDE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or // hide nav bar
                View.SYSTEM_UI_FLAG_FULLSCREEN or // hide status bar
                View.SYSTEM_UI_FLAG_IMMERSIVE
        } else {
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
        }
    }

    private val id: MutableLiveData<TweetId?> = MutableLiveData()
    internal val tweet: LiveData<TweetListItem?> = id.switchMap {
        if (it != null) tweetRepository.getTweetItem(it)
        else MutableLiveData()
    }
    internal val mediaItems: LiveData<List<MediaItem>> = tweet.map {
        it?.body?.mediaItems ?: listOf()
    }

    internal fun setTweetId(id: TweetId) {
        this.id.value = id
    }

    private val _systemUiVisibility = MutableLiveData(SYSTEM_UI_FLAG_SHOW)
    internal val isInImmersive: LiveData<Boolean> = _systemUiVisibility.map { v ->
        !isSystemUiVisible(v)
    }

    private fun isSystemUiVisible(v: Int?): Boolean =
        v?.let { SYSTEM_UI_FLAG_FULLSCREEN and it == 0 } ?: false

    internal val systemUiVisibility: LiveData<Int> = isInImmersive.map {
        if (it) {
            SYSTEM_UI_FLAG_HIDE
        } else {
            SYSTEM_UI_FLAG_SHOW
        }
    }

    internal fun onSystemUiVisibilityChange(visibility: Int) {
        _systemUiVisibility.value = visibility
    }

    fun toggleUiVisibility() {
        _systemUiVisibility.value = if (isSystemUiVisible(_systemUiVisibility.value)) {
            SYSTEM_UI_FLAG_HIDE
        } else {
            SYSTEM_UI_FLAG_SHOW
        }
    }

    private val _currentPosition = MutableLiveData<Int?>()
    internal val currentPosition: LiveData<Int?> = merge(
        _currentPosition, mediaItems
    ) { pos, items ->
        if (pos != null && pos < 0) {
            0
        } else if (pos != null && items?.isNotEmpty() == true) {
            min(pos, items.size - 1)
        } else {
            value
        }
    }

    fun setCurrentPosition(pos: Int) {
        _currentPosition.value = pos
    }

    val titleText: LiveData<String> = merge(
        _currentPosition.map { it?.plus(1) },
        mediaItems.map { it.size }
    ) { curPos, mediaSize ->
        if (curPos != null && mediaSize != null) {
            getApplication<Application>().resources.getString(
                R.string.media_current_position, curPos, mediaSize
            )
        } else {
            ""
        }
    }
}

@Module
interface MediaViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(MediaViewModel::class)
    fun bindMediaViewModel(viewModel: MediaViewModel): ViewModel

    companion object {
        @Provides
        fun provideMediaViewModel(
            tweetRepository: TweetRepository,
            application: Application
        ): MediaViewModel {
            return MediaViewModel(tweetRepository, application)
        }
    }
}
