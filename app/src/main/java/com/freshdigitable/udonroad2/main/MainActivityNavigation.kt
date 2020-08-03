package com.freshdigitable.udonroad2.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentArgs
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentArgs
import com.freshdigitable.udonroad2.user.UserActivityDirections
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
class MainActivityNavigation @Inject constructor(
    activity: MainActivity,
    actions: MainActivityAction
) : LifecycleEventObserver {

    private val navController: NavController by lazy {
        activity.findNavController(R.id.main_nav_host).apply {
            addOnDestinationChangedListener { _, destination, arguments ->
                containerState = MainNavHostState.create(destination, arguments)
            }
        }
    }
    private var containerState: MainNavHostState? = null

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
        actions.updateContainer.subscribe {
            activity.navigateTo(it)
        }.addTo(disposables)
        activity.lifecycle.addObserver(this)
    }

    private fun AppCompatActivity.navigateTo(nextState: MainNavHostState) {
        if (nextState == this@MainActivityNavigation.containerState) {
            return
        }
        when (nextState) {
            is MainNavHostState.Timeline -> toTimeline(nextState)
            is MainNavHostState.TweetDetail -> {
                navController.navigate(
                    ListItemFragmentDirections.actionTimelineToDetail(nextState.tweetId)
                )
            }
        }
    }

    private fun AppCompatActivity.toTimeline(nextState: MainNavHostState.Timeline) {
        when (nextState.cause) {
            MainNavHostState.Cause.INIT -> {
                navController.setGraph(
                    R.navigation.nav_main,
                    ListItemFragment.bundle(nextState.owner, getString(nextState.label))
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

        override val fragmentId: Int = R.id.fragment_timeline
    }

    data class TweetDetail(
        val tweetId: TweetId,
        override val cause: Cause = Cause.NAVIGATION
    ) : MainNavHostState() {
        override val fragmentId: Int = R.id.fragment_detail
    }

    abstract val fragmentId: Int
    abstract val cause: Cause

    enum class Cause { INIT, NAVIGATION, BACK }

    companion object
}

fun MainNavHostState.Companion.create(
    backStackEntry: NavBackStackEntry?
): MainNavHostState? = create(backStackEntry?.destination, backStackEntry?.arguments)

fun MainNavHostState.Companion.create(
    destination: NavDestination?,
    arguments: Bundle?
): MainNavHostState? {
    return when (destination?.id) {
        R.id.fragment_timeline -> {
            val args = ListItemFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.Timeline(
                ListOwner(args.ownerId, args.query),
                MainNavHostState.Cause.BACK
            )
        }
        R.id.fragment_detail -> {
            val args = TweetDetailFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.TweetDetail(args.tweetId, MainNavHostState.Cause.BACK)
        }
        else -> null
    }
}
