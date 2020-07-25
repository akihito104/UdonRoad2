package com.freshdigitable.udonroad2.main

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.user.UserActivityDirections
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class MainActivityNavigation @Inject constructor(
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
                        ListItemFragmentDirections.actionTimelineToDetail(containerState.tweetId)
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

sealed class MainActivityState : FragmentContainerState {
    data class Init(val type: QueryType) : MainActivityState()
    data class Timeline(val type: QueryType) : MainActivityState()
    data class TweetDetail(val tweetId: Long) : MainActivityState()
}
