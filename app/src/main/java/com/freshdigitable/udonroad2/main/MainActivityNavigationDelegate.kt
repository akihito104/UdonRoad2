package com.freshdigitable.udonroad2.main

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.di.ActivityScope
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.SnackbarFeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentArgs
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragmentDirections
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentArgs
import com.freshdigitable.udonroad2.timeline.fragment.TweetDetailFragmentDirections
import com.freshdigitable.udonroad2.user.UserActivityDirections
import java.io.Serializable
import javax.inject.Inject

@ActivityScope
internal class MainActivityNavigationDelegate @Inject constructor(
    mainActivity: MainActivity,
    private val state: MainActivityNavState,
) : ActivityEventDelegate,
    FeedbackMessageDelegate by SnackbarFeedbackMessageDelegate(
        weakRef(mainActivity) { it.findViewById(R.id.main_container) }
    ),
    LifecycleEventObserver {
    private val activity: MainActivity by weakRef(mainActivity)
    private val drawerLayout: DrawerLayout by weakRef(mainActivity) {
        it.findViewById<DrawerLayout>(R.id.main_drawer)
    }
    private val onDestinationChanged =
        NavController.OnDestinationChangedListener { nc, destination, arguments ->
            val containerState = requireNotNull(MainNavHostState.create(destination, arguments)) {
                "destination id is not registered at MainNavHostState: $destination"
            }
            state.setContainerState(containerState)
            state.setIsInTopLevelDest(destination.isTopLevelDestination(nc))
        }

    private fun NavDestination.isTopLevelDestination(nc: NavController): Boolean {
        val topLevelDestinations = AppBarConfiguration(nc.graph).topLevelDestinations
        var d: NavDestination? = this
        while (d != null) {
            if (topLevelDestinations.contains(d.id)) {
                return true
            }
            d = d.parent
        }
        return false
    }

    private val navController: NavController by weakRef(mainActivity) { a ->
        a.findNavController(R.id.main_nav_host).also {
            it.addOnDestinationChangedListener(onDestinationChanged)
        }
    }

    override fun dispatchNavHostNavigate(event: NavigationEvent) {
        when (event) {
            is TimelineEvent.Navigate.Timeline,
            is TimelineEvent.Navigate.Detail -> {
                activity.navigateTo(event)
            }
            is TimelineEvent.Navigate.UserInfo -> {
                navController.navigate(
                    UserActivityDirections.actionTimelineToActivityUser(event.tweetUserItem)
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
        if (state.containerState.value?.isDestinationEqualTo(nextState) == true) {
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
                    ListItemFragment.bundle(nextState.owner, nextState.label(this))
                )
            }
            NavigationEvent.Type.NAVIGATE -> {
                val dir = when (val container = state.containerState.value) {
                    is MainNavHostState.Timeline -> {
                        ListItemFragmentDirections.actionTimelineToTimeline(
                            nextState.owner.query,
                            nextState.owner.id,
                            nextState.label(this)
                        )
                    }
                    is MainNavHostState.TweetDetail -> {
                        TweetDetailFragmentDirections.actionDetailToTimeline(
                            nextState.owner.query,
                            nextState.owner.id,
                            nextState.label(this)
                        )
                    }
                    else -> throw AssertionError("undefined navigation at $container")
                }
                navController.navigate(dir)
            }
        }
    }

    fun onSupportNavigateUp(): Boolean = navController.navigateUp(drawerLayout)

    fun onNavDestinationSelected(item: MenuItem): Boolean =
        item.onNavDestinationSelected(navController)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY ->
                navController.removeOnDestinationChangedListener(onDestinationChanged)
            else -> Unit
        }
    }
}

typealias AppBarTitle = (Context) -> CharSequence

sealed class MainNavHostState : FragmentContainerState, Serializable {
    data class Timeline(
        val owner: ListOwner<*>,
        override val appBarTitle: AppBarTitle = { "" },
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
        override val appBarTitle: AppBarTitle = { it.getString(R.string.title_detail) }

        override fun isDestinationEqualTo(other: NavigationEvent?): Boolean {
            return (other as? TimelineEvent.Navigate.Detail)?.id == this.tweetId
        }
    }

    object Settings : MainNavHostState() {
        override val fragmentId: Int = R.id.fragment_settings
        override val appBarTitle: AppBarTitle = { it.getString(R.string.drawer_menu_settings) }
        override fun isDestinationEqualTo(other: NavigationEvent?): Boolean = false
    }

    abstract val fragmentId: Int
    abstract val appBarTitle: AppBarTitle
    abstract fun isDestinationEqualTo(other: NavigationEvent?): Boolean

    companion object
}

private fun TimelineEvent.Navigate.Timeline.label(context: Context): String {
    return when (val type = owner.query) {
        QueryType.Oauth -> context.getString(R.string.title_oauth)
        is QueryType.TweetQueryType.Timeline -> {
            if (type.userId == null) context.getString(R.string.title_home) else ""
        }
        is QueryType.TweetQueryType.Conversation -> context.getString(R.string.title_conversation)
        is QueryType.CustomTimelineListQueryType.Ownership ->
            context.getString(R.string.title_custom_timeline_ownership_list)
        is QueryType.TweetQueryType.CustomTimeline -> type.title
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
            MainNavHostState.Timeline(
                ListOwner(args.ownerId, args.query),
                appBarTitle = { args.label }
            )
        }
        R.id.fragment_detail -> {
            val args = TweetDetailFragmentArgs.fromBundle(requireNotNull(arguments))
            MainNavHostState.TweetDetail(args.tweetId)
        }
        R.id.fragment_settings -> MainNavHostState.Settings
        else -> null
    }
}

enum class NavigationIconType { MENU, UP, CLOSE }

@ActivityScope
internal class MainActivityNavState @Inject constructor() {
    private val _isInTopLevelDest = MutableLiveData<Boolean>()
    val isInTopLevelDest: LiveData<Boolean> = _isInTopLevelDest

    fun setIsInTopLevelDest(isInTop: Boolean) {
        _isInTopLevelDest.value = isInTop
    }

    private val _containerState = MutableLiveData<MainNavHostState>()
    val containerState: LiveData<MainNavHostState> = _containerState
    fun setContainerState(navHostState: MainNavHostState) {
        _containerState.value = navHostState
    }
}
