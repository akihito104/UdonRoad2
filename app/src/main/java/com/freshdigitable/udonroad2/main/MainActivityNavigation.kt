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
import androidx.navigation.ui.setupActionBarWithNavController
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
                is MainNavHostState.Timeline -> {
                    when (containerState.cause) {
                        MainNavHostState.Cause.INIT -> {
                            navController.setGraph(
                                R.navigation.nav_main,
                                ListItemFragment.bundle(
                                    containerState.type,
                                    activity.getString(containerState.label)
                                )
                            )
                            activity.setupActionBarWithNavController(navController)
                        }
                        MainNavHostState.Cause.NAVIGATION -> TODO()
                        MainNavHostState.Cause.BACK -> {
                            navController.popBackStack()
                        }
                    }
                }
                is MainNavHostState.TweetDetail -> {
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

sealed class MainNavHostState : FragmentContainerState {
    data class Timeline(val type: QueryType, override val cause: Cause) : MainNavHostState() {
        val label: Int = when (type) {
            QueryType.Oauth -> R.string.title_oauth
            is QueryType.TweetQueryType.Timeline -> {
                if (type.userId == null) R.string.title_home else 0
            }
            else -> TODO()
        }
    }

    data class TweetDetail(val tweetId: Long, override val cause: Cause = Cause.NAVIGATION) :
        MainNavHostState()

    abstract val cause: Cause

    enum class Cause { INIT, NAVIGATION, BACK }
}
