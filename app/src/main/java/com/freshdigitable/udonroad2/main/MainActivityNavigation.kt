package com.freshdigitable.udonroad2.main

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.Navigation
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.oauth.OauthEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.TimelineFragmentDirections
import com.freshdigitable.udonroad2.user.UserActivityDirections

class MainActivityNavigation(
    dispatcher: NavigationDispatcher,
    activity: AppCompatActivity,
    viewModelProvider: ViewModelProvider
) : Navigation<MainActivityState>(dispatcher, activity) {

    private val navController: NavController by lazy {
        activity.findNavController(R.id.main_nav_host)
    }
    val viewModel = viewModelProvider[MainViewModel::class.java]

    override fun onEvent(event: NavigationEvent): MainActivityState? {
        return when (event) {
            TimelineEvent.Init -> {
                navController.setGraph(
                    R.navigation.nav_main,
                    ListItemFragment.bundle(QueryType.TweetQueryType.Timeline())
                )
                MainActivityState.MainTimeline
            }
            is TimelineEvent.UserIconClicked -> {
                navController.navigate(UserActivityDirections.actionTimelineToActivityUser(event.user))
                null
            }
            is TimelineEvent.TweetDetailRequested -> {
                navController.navigate(TimelineFragmentDirections.actionTimelineToDetail(event.tweetId))
                MainActivityState.TweetDetail(event.tweetId)
            }
            is TimelineEvent.RetweetUserClicked -> {
                navController.navigate(UserActivityDirections.actionTimelineToActivityUser(event.user))
                null
            }
            is TimelineEvent.TweetItemSelected -> {
                viewModel.setSelectedItemId(event.selectedItemId)
                null
            }
            is TimelineEvent.MediaItemClicked -> {
                navController.navigate(
                    R.id.action_global_toMedia,
                    MediaActivityArgs(event.tweetId, event.index).toBundle()
                )
                null
            }
            OauthEvent.Init -> {
                navController.setGraph(
                    R.navigation.nav_oauth,
                    ListItemFragment.bundle(QueryType.Oauth)
                )
                MainActivityState.Oauth
            }
            is OauthEvent.OauthRequested -> {
                val intent = Intent(ACTION_VIEW, Uri.parse(event.authUrl))
                activity.startActivity(intent)
                null
            }
            is OauthEvent.OauthSucceeded -> {
                navController.setGraph(
                    R.navigation.nav_main,
                    ListItemFragment.bundle(QueryType.TweetQueryType.Timeline())
                )
                MainActivityState.MainTimeline
            }
            TimelineEvent.Back -> {
                if (currentState is MainActivityState.TweetDetail) {
                    navController.popBackStack()
                    null
                } else if (currentState is MainActivityState.MainTimeline &&
                    viewModel.isFabVisible.value == true
                ) {
                    viewModel.setFabVisible(false)
                    null
                } else {
                    MainActivityState.Halt
                }
            }
            else -> null
        }
    }

    override fun navigate(s: MainActivityState?) {
        when (s) {
            is MainActivityState.MainTimeline -> {
                viewModel.setFabVisible(true)
            }
            is MainActivityState.TweetDetail -> {
                viewModel.setFabVisible(false)
            }
            is MainActivityState.Oauth -> {
                viewModel.setFabVisible(false)
            }
            MainActivityState.Halt -> activity.finish()
        }
    }
}

sealed class MainActivityState : FragmentContainerState {
    object MainTimeline : MainActivityState()

    data class TweetDetail(val tweetId: Long) : MainActivityState()

    object Oauth : MainActivityState()

    object Halt : MainActivityState()
}
