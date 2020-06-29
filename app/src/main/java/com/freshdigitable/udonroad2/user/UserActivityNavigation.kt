package com.freshdigitable.udonroad2.user

import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.Navigation
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class UserActivityNavigation(
    navigator: NavigationDispatcher,
    activity: UserActivity,
    viewModelProvider: ViewModelProvider
) : Navigation<UserActivityState>(navigator, activity) {

    private val viewModel = viewModelProvider[UserViewModel::class.java]

    override fun onEvent(event: NavigationEvent): UserActivityState? {
        when (event) {
            is TimelineEvent.TweetItemSelected -> viewModel.setSelectedItemId(event.selectedItemId)
            is TimelineEvent.UserIconClicked -> UserActivity.start(
                activity,
                event.user
            )
        }
        return null
    }

    override fun navigate(s: UserActivityState?) {}
}

class UserActivityState : FragmentContainerState
