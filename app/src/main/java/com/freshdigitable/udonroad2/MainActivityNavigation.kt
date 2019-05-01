package com.freshdigitable.udonroad2

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.freshdigitable.udonroad2.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.TimelineFragment
import com.freshdigitable.udonroad2.timeline.TweetDetailFragment

class MainActivityNavigation(
    dispatcher: NavigationDispatcher,
    activity: AppCompatActivity,
    @IdRes containerId: Int
) : Navigation<MainActivityState>(dispatcher, activity, containerId) {

    override fun onEvent(event: NavigationEvent): MainActivityState? {
        return when (event) {
            TimelineEvent.Init -> MainActivityState.MainTimeline
            is TimelineEvent.UserIconClicked -> {
                UserActivity.start(activity, event.userId)
                null
            }
            is TimelineEvent.TweetDetailRequested -> {
                MainActivityState.TweetDetail(event.tweetId)
            }
            TimelineEvent.Back -> {
                if (currentState is MainActivityState.TweetDetail) {
                    MainActivityState.MainTimeline
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
                replace(TimelineFragment())
            }
            is MainActivityState.TweetDetail -> {
                replace(TweetDetailFragment.newInstance(s.tweetId))
            }
            MainActivityState.Halt -> activity.finish()
        }
    }
}

sealed class MainActivityState : FragmentContainerState {
    object MainTimeline : MainActivityState()

    data class TweetDetail(val tweetId: Long) : MainActivityState()

    object Halt : MainActivityState()
}
