package com.freshdigitable.udonroad2.user

import androidx.lifecycle.ViewModelProvider
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.Navigation
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class UserActivityNavigation(
    navigator: EventDispatcher,
    activity: UserActivity,
    viewModelProvider: ViewModelProvider
) : Navigation<UserActivityState>(navigator, activity) {

    private val viewModel = viewModelProvider[UserViewModel::class.java]

    override fun onEvent(event: AppEvent): UserActivityState? {
        when (event) {
            is TimelineEvent.TweetItemSelection.Selected -> viewModel.setSelectedItemId(
                event.selectedItemId
            )
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
