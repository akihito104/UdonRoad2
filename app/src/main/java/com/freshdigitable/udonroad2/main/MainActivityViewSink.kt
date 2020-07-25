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

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.observe
import androidx.lifecycle.toLiveData
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.user.UserActivityDirections
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@ActivityScope
class MainActivityViewSink @Inject constructor(
    stateModel: MainActivityStateModel
) {
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
            selectedItem = selectedItemId as SelectedItemId,
            fabVisible = isFabVisible as Boolean
        )
    }
        .toFlowable(BackpressureStrategy.BUFFER)
        .toLiveData()
}

data class MainActivityViewState(
    val title: String,
    val selectedItem: SelectedItemId?,
    val fabVisible: Boolean,
    override val containerState: MainActivityState
) : ViewState

class MainActivityNav @Inject constructor(
    activity: MainActivity,
    actions: MainActivityAction,
    viewSink: MainActivityViewSink
) : LifecycleEventObserver {
    private val navController: NavController by lazy {
        activity.findNavController(R.id.main_nav_host)
    }

    private val disposables = CompositeDisposable()

    init {
        actions.authApp.subscribe {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.authUrl))
            activity.startActivity(intent)
        }.addTo(disposables)
        actions.launchUserInfo.subscribe {
            navController.navigate(UserActivityDirections.actionTimelineToActivityUser(it))
        }.addTo(disposables)
        actions.launchMediaViewer.subscribe {
            navController.navigate(
                R.id.action_global_toMedia,
                MediaActivityArgs(it.tweetId, it.index).toBundle()
            )
        }.addTo(disposables)
        actions.rollbackViewState.subscribe {
            navController.popBackStack()
        }.addTo(disposables)
        activity.lifecycle.addObserver(this)

        viewSink.state.map { it.containerState }.distinctUntilChanged().observe(activity) {
            when (val containerState = it) {
                is MainActivityState.Init -> {
                    navController.setGraph(
                        R.navigation.nav_main,
                        ListItemFragment.bundle(containerState.type)
                    )
                }
                is MainActivityState.TweetDetail -> {
                    navController.navigate(
                        ListItemFragmentDirections.actionTimelineToDetail(
                            containerState.tweetId
                        )
                    )
                }
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            disposables.clear()
        }
    }

    private fun Disposable.addTo(compositeDisposable: CompositeDisposable) {
        compositeDisposable.add(this)
    }
}
