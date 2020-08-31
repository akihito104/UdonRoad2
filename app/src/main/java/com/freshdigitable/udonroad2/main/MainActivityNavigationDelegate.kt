package com.freshdigitable.udonroad2.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDelegate
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentArgs
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentArgs
import com.freshdigitable.udonroad2.user.UserActivityDirections
import com.google.android.material.snackbar.Snackbar
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
class MainActivityNavigationDelegate @Inject constructor(
    mainActivity: MainActivity,
) : NavigationDelegate(mainActivity), ActivityEventDelegate {
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
                MainNavHostState.create(destination, arguments)
            )
            _containerState.value = containerState
        }

    private val _containerState = MutableLiveData<MainNavHostState>()
    val containerState: LiveData<MainNavHostState> = _containerState

    override fun dispatchNavHostNavigate(event: NavigationEvent) {
        when (event) {
            is TimelineEvent.Navigate.Timeline,
            is TimelineEvent.Navigate.Detail -> {
                activity.navigateTo(event)
            }
            is TimelineEvent.Navigate.UserInfo -> {
                navController.navigate(
                    UserActivityDirections.actionTimelineToActivityUser(event.tweetingUser)
                )
            }
            is TimelineEvent.Navigate.MediaViewer -> {
                navController.navigate(
                    R.id.action_global_toMedia,
                    MediaActivityArgs(event.tweetId, event.index).toBundle()
                )
            }
        }
    }

    private fun AppCompatActivity.navigateTo(nextState: NavigationEvent) {
        if (containerState.value?.isDestinationEqualTo(nextState) == true) {
            return
        }
        when (nextState) {
            is TimelineEvent.Navigate.Timeline -> toTimeline(nextState)
            is TimelineEvent.Navigate.Detail -> {
                navController.navigate(
                    ListItemFragmentDirections.actionTimelineToDetail(nextState.id)
                )
            }
        }
    }

    private fun AppCompatActivity.toTimeline(nextState: TimelineEvent.Navigate.Timeline) {
        when (nextState.type) {
            NavigationEvent.Type.INIT -> {
                navController.setGraph(
                    R.navigation.nav_main,
                    ListItemFragment.bundle(nextState.owner, getString(nextState.label))
                )
                setupActionBarWithNavController(navController, drawerLayout)
            }
            NavigationEvent.Type.NAVIGATE -> TODO()
        }
    }

    fun dispatchBack() {
        activity.onBackPressedDispatcher.onBackPressed()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        super.onStateChanged(source, event)
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                navController.addOnDestinationChangedListener(onDestinationChanged)
            }
            Lifecycle.Event.ON_DESTROY -> {
                navController.removeOnDestinationChangedListener(onDestinationChanged)
            }
            else -> Unit
        }
    }

    fun onSupportNavigateUp(): Boolean = navController.navigateUp(drawerLayout)

    private val snackbarContainer: View by weakRef(activity) {
        it.findViewById(R.id.main_container)
    }

    override fun dispatchFeedbackMessage(message: FeedbackMessage) {
        if (message.args == null) {
            Snackbar.make(snackbarContainer, message.messageRes, Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(
                snackbarContainer,
                activity.getString(message.messageRes, message.args),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}

sealed class MainNavHostState : FragmentContainerState, Serializable {
    data class Timeline(
        val owner: ListOwner<*>
    ) : MainNavHostState() {
        override val fragmentId: Int = R.id.fragment_timeline

        override fun isDestinationEqualTo(other: NavigationEvent?): Boolean {
            return (other as? TimelineEvent.Navigate.Timeline)?.owner == this.owner
        }
    }

    data class TweetDetail(
        val tweetId: TweetId
    ) : MainNavHostState() {
        override val fragmentId: Int = R.id.fragment_detail

        override fun isDestinationEqualTo(other: NavigationEvent?): Boolean {
            return (other as? TimelineEvent.Navigate.Detail)?.id == this.tweetId
        }
    }

    abstract val fragmentId: Int
    abstract fun isDestinationEqualTo(other: NavigationEvent?): Boolean

    companion object
}

private val TimelineEvent.Navigate.Timeline.label: Int
    get() {
        return when (val type = owner.query) {
            QueryType.Oauth -> R.string.title_oauth
            is QueryType.TweetQueryType.Timeline -> {
                if (type.userId == null) R.string.title_home else 0
            }
            else -> TODO()
        }
    }

private fun MainNavHostState.Companion.create(
    destination: NavDestination?,
    arguments: Bundle?
): MainNavHostState? {
    return when (destination?.id) {
        R.id.fragment_timeline -> {
            val args = ListItemFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.Timeline(ListOwner(args.ownerId, args.query))
        }
        R.id.fragment_detail -> {
            val args = TweetDetailFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.TweetDetail(args.tweetId)
        }
        else -> null
    }
}
