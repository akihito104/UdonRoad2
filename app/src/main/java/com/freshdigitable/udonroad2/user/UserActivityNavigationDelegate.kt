package com.freshdigitable.udonroad2.user

import android.view.View
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivity
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectDelegate
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.SnackbarFeedbackMessageDelegate
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.app.weakRef

class UserActivityNavigationDelegate(
    userActivity: UserActivity,
) : ActivityEffectDelegate {
    private val feedbackDelegate = SnackbarFeedbackMessageDelegate(
        weakRef(userActivity) { it.findViewById<View>(R.id.user_pager).parent as View }
    )
    private val activity: UserActivity by weakRef(userActivity)

    override fun accept(event: AppEffect) {
        when (event) {
            is TimelineEffect.Navigate.UserInfo -> UserActivity.start(activity, event.tweetUserItem)
            is TimelineEffect.Navigate.MediaViewer -> MediaActivity.start(
                activity,
                MediaActivityArgs(event.tweetId, event.index)
            )
            is FeedbackMessage -> feedbackDelegate.dispatchFeedbackMessage(event)
        }
    }
}
