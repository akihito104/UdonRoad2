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

package com.freshdigitable.udonroad2.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.StateHolder
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.timeline.viewmodel.FragmentContainerViewSink
import io.reactivex.BackpressureStrategy
import javax.inject.Inject

@ActivityScope
class MainActivityViewSink @Inject constructor(
    stateModel: MainActivityStateModel
) : FragmentContainerViewSink {
    val state: LiveData<MainActivityViewState> = AppViewState.combineLatest(
        listOf(
            stateModel.containerState,
            stateModel.title,
            stateModel.selectedItemId,
            stateModel.isFabVisible
        )
    ) { (containerState, title, selectedItemId, isFabVisible) ->
        MainActivityViewState(
            containerState = containerState as MainActivityState,
            title = title as String,
            selectedItem = (selectedItemId as StateHolder<SelectedItemId>).value,
            fabVisible = isFabVisible as Boolean
        )
    }
        .toFlowable(BackpressureStrategy.BUFFER)
        .toLiveData()

    override val containerState: LiveData<SelectedItemId?> = state.map { it.selectedItem }
}

data class MainActivityViewState(
    val title: String,
    val selectedItem: SelectedItemId?,
    val fabVisible: Boolean,
    override val containerState: MainActivityState
) : ViewState
