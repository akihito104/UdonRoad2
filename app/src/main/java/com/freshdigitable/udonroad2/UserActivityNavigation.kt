package com.freshdigitable.udonroad2

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.freshdigitable.udonroad2.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.navigation.Navigation
import com.freshdigitable.udonroad2.navigation.NavigationDispatcher
import com.freshdigitable.udonroad2.navigation.NavigationEvent
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class UserActivityNavigation(
    navigator: NavigationDispatcher,
    activity: UserActivity,
    @IdRes containerId: Int,
    viewModelFactory: ViewModelProvider.Factory
) : Navigation<UserActivityState>(navigator, activity, containerId) {

    private val viewModel =
        ViewModelProviders.of(activity, viewModelFactory)
            .get(UserViewModel::class.java)

    override fun onEvent(event: NavigationEvent): UserActivityState? {
        if (event is TimelineEvent.TweetItemSelected) {
            viewModel.setSelectedItemId(event.selectedItemId)
        }
        return null
    }

    override fun navigate(s: UserActivityState?) {}
}

class UserActivityState : FragmentContainerState
