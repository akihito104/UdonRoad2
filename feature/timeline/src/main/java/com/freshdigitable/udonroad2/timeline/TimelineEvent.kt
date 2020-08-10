package com.freshdigitable.udonroad2.timeline

import android.view.MenuItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.timeline.TimelineEvent.SelectedItemShortcut
import java.io.Serializable

sealed class TimelineEvent : NavigationEvent {
    data class Setup(val savedState: Serializable? = null) : TimelineEvent()

    object Init : TimelineEvent()

    data class DestinationChanged(val state: FragmentContainerState) : NavigationEvent

    data class UserIconClicked(val user: TweetingUser) : TimelineEvent()

    data class RetweetUserClicked(val user: TweetingUser) : TimelineEvent()

    sealed class TweetItemSelection : TimelineEvent() {
        data class Selected(val selectedItemId: SelectedItemId) : TweetItemSelection()
        data class Unselected(val owner: ListOwner<*>) : TweetItemSelection()
        data class Toggle(val item: SelectedItemId) : TweetItemSelection()
    }

    sealed class SelectedItemShortcut : TimelineEvent() {
        data class TweetDetail(override val tweetId: TweetId) : SelectedItemShortcut()
        data class Like(override val tweetId: TweetId) : SelectedItemShortcut()
        data class Retweet(override val tweetId: TweetId) : SelectedItemShortcut()

        abstract val tweetId: TweetId

        companion object
    }

    data class MemberListClicked(val memberList: MemberListItem) : TimelineEvent()

    data class MediaItemClicked(
        val tweetId: TweetId,
        val index: Int = 0,
        val selectedItemId: SelectedItemId? = null
    ) : TimelineEvent()
}

fun SelectedItemShortcut.Companion.create(
    menuItem: MenuItem,
    selectedItemId: SelectedItemId
): Collection<TimelineEvent> {
    val tweetId = selectedItemId.quoteId ?: selectedItemId.originalId
    return when (menuItem.itemId) {
        R.id.iffabMenu_main_detail -> listOf(SelectedItemShortcut.TweetDetail(tweetId))
        R.id.iffabMenu_main_fav -> listOf(SelectedItemShortcut.Like(tweetId))
        R.id.iffabMenu_main_rt -> listOf(SelectedItemShortcut.Retweet(tweetId))
        R.id.iffabMenu_main_favRt -> listOf(
            SelectedItemShortcut.Like(tweetId),
            SelectedItemShortcut.Retweet(tweetId)
        )
        else -> TODO()
    }
}
