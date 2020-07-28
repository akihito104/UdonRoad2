package com.freshdigitable.udonroad2.main

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentArgs
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentArgs
import com.freshdigitable.udonroad2.user.UserActivityDirections
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.io.Serializable
import javax.inject.Inject

class MainActivityNavigation @Inject constructor(
    activity: MainActivity,
    actions: MainActivityAction,
    state: MainActivityStateModel
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
            activity.onBackPressedDispatcher.onBackPressed()
        }.addTo(disposables)
        activity.lifecycle.addObserver(this)

        state.containerState.observe(activity) { activity.navigateTo(it) }
    }

    private fun AppCompatActivity.navigateTo(containerState: MainNavHostState) {
        when (containerState) {
            is MainNavHostState.Timeline -> toTimeline(containerState)
            is MainNavHostState.TweetDetail -> {
                navController.navigate(
                    ListItemFragmentDirections.actionTimelineToDetail(containerState.tweetId)
                )
            }
        }
    }

    private fun AppCompatActivity.toTimeline(containerState: MainNavHostState.Timeline) {
        when (containerState.cause) {
            MainNavHostState.Cause.INIT -> {
                navController.setGraph(
                    R.navigation.nav_main,
                    ListItemFragment.bundle(containerState.owner, getString(containerState.label))
                )
                setupActionBarWithNavController(navController)
            }
            MainNavHostState.Cause.NAVIGATION -> TODO()
            MainNavHostState.Cause.BACK -> {
                navController.popBackStack()
            }
        }
    }

    val prevNavHostState: MainNavHostState?
        get() = MainNavHostState.create(navController.previousBackStackEntry)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            disposables.clear()
        }
    }

    private fun Disposable.addTo(compositeDisposable: CompositeDisposable) {
        compositeDisposable.add(this)
    }
}

sealed class MainNavHostState : FragmentContainerState, Serializable {
    data class Timeline(
        val owner: ListOwner<*>,
        override val cause: Cause
    ) : MainNavHostState() {
        val label: Int = when (val type = owner.query) {
            QueryType.Oauth -> R.string.title_oauth
            is QueryType.TweetQueryType.Timeline -> {
                if (type.userId == null) R.string.title_home else 0
            }
            else -> TODO()
        }
    }

    data class TweetDetail(
        val tweetId: Long,
        override val cause: Cause = Cause.NAVIGATION
    ) : MainNavHostState()

    abstract val cause: Cause

    enum class Cause { INIT, NAVIGATION, BACK }

    companion object
}

fun MainNavHostState.Companion.create(backStackEntry: NavBackStackEntry?): MainNavHostState? {
    return when (backStackEntry?.destination?.id) {
        R.id.fragment_timeline -> {
            val args =
                ListItemFragmentArgs.fromBundle(requireNotNull(backStackEntry.arguments))
            MainNavHostState.Timeline(
                ListOwner(args.ownerId, args.query),
                MainNavHostState.Cause.BACK
            )
        }
        R.id.fragment_detail -> {
            val args =
                TweetDetailFragmentArgs.fromBundle(requireNotNull(backStackEntry.arguments))
            MainNavHostState.TweetDetail(args.tweetId, MainNavHostState.Cause.BACK)
        }
        else -> null
    }
}
