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

import android.os.Build
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.di.ViewModelKey
import com.freshdigitable.udonroad2.model.app.ext.merge
import com.freshdigitable.udonroad2.model.app.navigation.onNull
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import kotlin.math.min

class MediaViewModel @Inject constructor(
    tweetRepository: TweetRepository,
    executor: AppExecutor,
) : ViewModel() {
    private val id: MutableLiveData<TweetId?> = MutableLiveData()
    internal val tweet: LiveData<TweetListItem?> = id.switchMap {
        liveData(executor.dispatcher.mainContext) {
            if (it == null) {
                return@liveData
            }
            emitSource(tweetRepository.getTweetItemSource(it).onNull(
                executor = executor,
                onNull = { tweetRepository.findTweetListItem(it) },
                onError = { }
            ))
        }
    }
    val mediaItems: LiveData<List<MediaItem>> = tweet.map {
        it?.body?.mediaItems ?: listOf()
    }

    internal fun setTweetId(id: TweetId) {
        this.id.value = id
    }

    private val _systemUiVisibility = MutableLiveData(SystemUiVisibility.SHOW)
    internal val systemUiVisibility: LiveData<SystemUiVisibility> = _systemUiVisibility
    internal val isInImmersive: LiveData<Boolean> = _systemUiVisibility.map {
        it == SystemUiVisibility.HIDE
    }

    internal fun onSystemUiVisibilityChange(visibility: Int) {
        _systemUiVisibility.value = SystemUiVisibility.get(visibility)
    }

    fun toggleUiVisibility() {
        val current = _systemUiVisibility.value ?: return
        _systemUiVisibility.value = current.toggle()
    }

    private val _currentPosition = MutableLiveData<Int?>()
    val currentPosition: LiveData<Int?> = merge(
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
}

enum class SystemUiVisibility(val visibility: Int) {
    SHOW(
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            else -> -1
        }
    ),
    HIDE(
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or // hide nav bar
                    View.SYSTEM_UI_FLAG_FULLSCREEN or // hide status bar
                    View.SYSTEM_UI_FLAG_IMMERSIVE
            }
            else -> View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
        }
    ),
    ;

    fun toggle(): SystemUiVisibility = when (this) {
        SHOW -> HIDE
        else -> SHOW
    }

    companion object {
        private val FULLSCREEN = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ->
                View.SYSTEM_UI_FLAG_FULLSCREEN
            else -> View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

        fun get(visibility: Int): SystemUiVisibility = when {
            visibility and FULLSCREEN == 0 -> HIDE
            else -> SHOW
        }
    }
}

@Module
interface MediaViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(MediaViewModel::class)
    fun bindMediaViewModel(viewModel: MediaViewModel): ViewModel
}
