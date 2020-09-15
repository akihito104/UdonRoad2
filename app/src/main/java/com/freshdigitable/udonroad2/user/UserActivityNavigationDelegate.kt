package com.freshdigitable.udonroad2.user

import android.view.View
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivity
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventDelegate
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.navigation.NavigationDelegate
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.SnackbarFeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.weakRef
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class UserActivityNavigationDelegate(
    userActivity: UserActivity,
) : NavigationDelegate(userActivity), ActivityEventDelegate,
    FeedbackMessageDelegate by SnackbarFeedbackMessageDelegate(
        weakRef(userActivity) { it.findViewById<View>(R.id.user_pager).parent as View }
    ) {
    private val activity: UserActivity by weakRef(userActivity)

    override fun dispatchNavHostNavigate(event: NavigationEvent) {
        when (event) {
            is TimelineEvent.Navigate.UserInfo -> UserActivity.start(activity, event.tweetingUser)
            is TimelineEvent.Navigate.MediaViewer -> MediaActivity.start(
                activity,
                MediaActivityArgs(event.tweetId, event.index)
            )
        }
    }
}
