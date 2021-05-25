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

package com.freshdigitable.udonroad2.input

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.freshdigitable.udonroad2.input.MediaChooserResultContract.MediaChooserResult
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

internal class TweetInputViewModel @Inject constructor(
    viewModelSource: TweetInputViewModelSource,
) : ViewModel(), TweetInputEventListener by viewModelSource {
    val state = viewModelSource.state.asLiveData(viewModelScope.coroutineContext)
    val isExpanded: LiveData<Boolean> =
        viewModelSource.isExpanded.asLiveData(viewModelScope.coroutineContext)
    val user: LiveData<UserEntity?> =
        viewModelSource.user.asLiveData(viewModelScope.coroutineContext)
    internal val menuItem = state.map { it.menuItem }.distinctUntilChanged()

    val expandAnimationEvent: Flow<TweetInputEvent.Opened> = viewModelSource.expandAnimationEvent
    internal val chooserForCameraApp = viewModelSource.chooserForCameraApp
}

internal interface TweetInputEventListener {
    val openInput: AppEventListener
    val updateText: AppEventListener1<Editable>
    val sendTweet: AppEventListener1<InputTweet>
    val cancelInput: AppEventListener
    val startInput: AppEventListener
    fun onCameraAppCandidatesQueried(candidates: List<Components>, path: AppFilePath)
    val updateMedia: AppEventListener1<MediaChooserResult>
}

interface CameraAppEventListener {
    val chooseCameraApp: AppEventListener1<Components>
}

@ActivityScope
class TweetInputSharedState @Inject constructor() {
    internal val taskStateSource = MutableStateFlow<InputViewState?>(null)
    val isExpanded: Flow<Boolean> = taskStateSource.mapLatest { it?.isExpanded == true }
        .distinctUntilChanged()
}
