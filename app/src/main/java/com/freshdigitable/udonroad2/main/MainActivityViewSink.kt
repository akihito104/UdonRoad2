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
import androidx.lifecycle.observe
import androidx.lifecycle.toLiveData
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.OAuthTokenRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.ViewState
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.user.UserActivityDirections
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import javax.inject.Inject

class MainActivityStateModel @Inject constructor(
    action: MainActivityAction,
    tokenRepository: OAuthTokenRepository,
    selectedItemRepository: SelectedItemRepository
) {
    private val firstContainerState: Flowable<MainActivityState> = action.showFirstView.map {
        Timber.tag("StateModel").d("firstContainerState: $it")
        when {
            tokenRepository.getCurrentUserId() != null -> {
                tokenRepository.login()
                MainActivityState.Init(QueryType.TweetQueryType.Timeline())
            }
            else -> MainActivityState.Init(QueryType.Oauth)
        }
    }
    val containerState: Flowable<MainActivityState> = Flowable.merge(
        firstContainerState,
        action.showTimeline.map {
            when (it) {
                is QueryType.TweetQueryType.Timeline,
                is QueryType.Oauth -> MainActivityState.Timeline(it)
                else -> TODO("not implemented")
            }
        },
        action.showTweetDetail.map { MainActivityState.TweetDetail(it.tweetId) }
    )

    val selectedItemId: Flowable<SelectedItemId> = Flowable.merge(
        action.changeItemSelectState.map {
            selectedItemRepository.put(it.selectedItemId)
            selectedItemRepository.find(it.selectedItemId.owner)
                ?: SelectedItemId(it.selectedItemId.owner, null)
        },
        action.toggleSelectedItem.map {
            val current = selectedItemRepository.find(it.item.owner)
            when (it.item) {
                current -> selectedItemRepository.remove(it.item.owner)
                else -> selectedItemRepository.put(it.item)
            }
            selectedItemRepository.find(it.item.owner) ?: SelectedItemId(it.item.owner, null)
        }
    )

    val isFabVisible: Flowable<Boolean> = selectedItemId.map { it.originalId != null }

    val title: Flowable<String> = containerState
        .filter { it is MainActivityState.Init || it is MainActivityState.Timeline || it is MainActivityState.TweetDetail }
        .map {
            when (it) {
                is MainActivityState.Init, is MainActivityState.Timeline -> {
                    val queryType = when (it) {
                        is MainActivityState.Init -> it.type
                        is MainActivityState.Timeline -> it.type
                        else -> throw IllegalStateException()
                    }
                    when (queryType) {
                        is QueryType.TweetQueryType.Timeline -> "Home"
                        is QueryType.Oauth -> "Welcome"
                        else -> throw IllegalStateException()
                    }
                }
                is MainActivityState.TweetDetail -> "Tweet"
            }
        }
}

class MainActivityViewSink @Inject constructor(
    stateModel: MainActivityStateModel
) {
    val state: LiveData<MainActivityViewState> = Flowable.combineLatest(
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
//        .toFlowable(BackpressureStrategy.BUFFER)
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

        viewSink.state.observe(activity) {
            when (val containerState = it.containerState) {
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
