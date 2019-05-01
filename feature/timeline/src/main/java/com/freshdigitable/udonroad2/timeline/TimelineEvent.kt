package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.navigation.NavigationEvent

sealed class TimelineEvent : NavigationEvent {
    object Init : TimelineEvent()

    data class TweetDetailRequested(val tweetId: Long) : TimelineEvent()

    data class UserIconClicked(val userId: Long) : TimelineEvent()

    object Back : TimelineEvent()
}
