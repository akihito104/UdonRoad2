package com.freshdigitable.udonroad2.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.addTo
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.oauth.OauthNavigationDelegate
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentArgs
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentArgs
import com.freshdigitable.udonroad2.user.UserActivityDirections
import io.reactivex.disposables.CompositeDisposable
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
class MainActivityNavigationDelegate @Inject constructor(
    mainActivity: MainActivity,
    private val oauthNavigation: OauthNavigationDelegate,
    actions: MainActivityActions
) : LifecycleEventObserver {
    private val activity: MainActivity by weakRef(mainActivity)
    private val drawerLayout: DrawerLayout by weakRef(mainActivity) {
        it.findViewById<DrawerLayout>(R.id.main_drawer)
    }
    private val navController: NavController by weakRef(mainActivity) {
        it.findNavController(R.id.main_nav_host)
    }

    private val onDestinationChanged =
        NavController.OnDestinationChangedListener { _, destination, arguments ->
            val containerState = requireNotNull(
                MainNavHostState.create(
                    destination,
                    arguments,
                    MainNavHostState.Cause.DESTINATION_CHANGED
                )
            )
            this@MainActivityNavigationDelegate.containerState = containerState
            actions.dispatcher.postEvent(TimelineEvent.DestinationChanged(containerState))
        }

    private var containerState: MainNavHostState? = null

    private val disposables = CompositeDisposable()

    init {
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
        if (nextState.isDestinationEqualTo(this@MainActivityNavigationDelegate.containerState)) {
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
                setupActionBarWithNavController(navController, drawerLayout)
            }
            MainNavHostState.Cause.NAVIGATION -> TODO()
            MainNavHostState.Cause.DESTINATION_CHANGED -> Unit
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                navController.addOnDestinationChangedListener(onDestinationChanged)
            }
            Lifecycle.Event.ON_DESTROY -> {
                navController.removeOnDestinationChangedListener(onDestinationChanged)
                disposables.clear()
                activity.lifecycle.removeObserver(this)
            }
            else -> Unit
        }
    }

    fun onSupportNavigateUp(): Boolean = navController.navigateUp(drawerLayout)
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

        override fun isDestinationEqualTo(other: MainNavHostState?): Boolean {
            return (other as? Timeline)?.owner == this.owner
        }
    }

    data class TweetDetail(
        val tweetId: TweetId,
        override val cause: Cause = Cause.NAVIGATION
    ) : MainNavHostState() {
        override val fragmentId: Int = R.id.fragment_detail

        override fun isDestinationEqualTo(other: MainNavHostState?): Boolean {
            return (other as? TweetDetail)?.tweetId == this.tweetId
        }
    }

    abstract val fragmentId: Int
    abstract val cause: Cause
    abstract fun isDestinationEqualTo(other: MainNavHostState?): Boolean

    enum class Cause { INIT, NAVIGATION, DESTINATION_CHANGED }

    companion object
}

fun MainNavHostState.Companion.create(
    destination: NavDestination?,
    arguments: Bundle?,
    cause: MainNavHostState.Cause
): MainNavHostState? {
    return when (destination?.id) {
        R.id.fragment_timeline -> {
            val args = ListItemFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.Timeline(ListOwner(args.ownerId, args.query), cause)
        }
        R.id.fragment_detail -> {
            val args = TweetDetailFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.TweetDetail(args.tweetId, cause)
        }
        else -> null
    }
}
