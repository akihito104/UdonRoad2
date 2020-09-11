package com.freshdigitable.udonroad2.user

import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDelegate
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class UserActivityNavigation(
    userActivity: UserActivity,
) : NavigationDelegate(userActivity), ActivityEventDelegate {
    private val activity: UserActivity by weakRef(userActivity)

    override fun dispatchNavHostNavigate(event: NavigationEvent) {
        when (event) {
            is TimelineEvent.Navigate.UserInfo -> UserActivity.start(activity, event.tweetingUser)
        }
    }

    override fun dispatchFeedbackMessage(message: FeedbackMessage) {
        TODO("Not yet implemented")
    }
}
