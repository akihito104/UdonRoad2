package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import java.io.Serializable

sealed class TimelineEvent : NavigationEvent {
    data class Setup(val savedState: Serializable? = null) : TimelineEvent()

    object Init : TimelineEvent()

    data class PopUpTo(val state: FragmentContainerState) : NavigationEvent

    data class TweetDetailRequested(val tweetId: Long) : TimelineEvent()

    data class UserIconClicked(val user: TweetingUser) : TimelineEvent()

    data class RetweetUserClicked(val user: TweetingUser) : TimelineEvent()

    data class TweetItemSelected(val selectedItemId: SelectedItemId) : TimelineEvent()

    data class ToggleTweetItemSelectedState(val item: SelectedItemId) : TimelineEvent()

    data class MemberListClicked(val memberList: MemberListItem) : TimelineEvent()

    data class MediaItemClicked(
        val tweetId: Long,
        val index: Int = 0,
        val selectedItemId: SelectedItemId? = null
    ) : TimelineEvent()
}
