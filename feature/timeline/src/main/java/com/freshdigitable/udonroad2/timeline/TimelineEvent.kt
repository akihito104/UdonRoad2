package com.freshdigitable.udonroad2.timeline

import android.view.MenuItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.timeline.TimelineEvent.SelectedItemShortcut
import java.io.Serializable

sealed class TimelineEvent : AppEvent {
    data class Setup(val savedState: Serializable? = null) : TimelineEvent()

    object Init : TimelineEvent()

    data class UserIconClicked(val user: TweetingUser) : TimelineEvent()

    data class RetweetUserClicked(val user: TweetingUser) : TimelineEvent()

    sealed class TweetItemSelection : TimelineEvent() {
        data class Selected(val selectedItemId: SelectedItemId) : TweetItemSelection() {
            override val owner: ListOwner<*> get() = selectedItemId.owner
        }

        data class Unselected(override val owner: ListOwner<*>) : TweetItemSelection()
        data class Toggle(val item: SelectedItemId) : TweetItemSelection() {
            override val owner: ListOwner<*> get() = item.owner
        }

        abstract val owner: ListOwner<*>
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

    sealed class Navigate : TimelineEvent(), NavigationEvent {
        data class Timeline(
            val owner: ListOwner<*>,
            override val type: NavigationEvent.Type = NavigationEvent.Type.NAVIGATE
        ) : Navigate()

        data class Detail(
            val id: TweetId,
            override val type: NavigationEvent.Type = NavigationEvent.Type.NAVIGATE
        ) : Navigate()

        data class UserInfo(val tweetingUser: TweetingUser) : Navigate() {
            override val type: NavigationEvent.Type = NavigationEvent.Type.NAVIGATE
        }

        data class MediaViewer(
            val tweetId: TweetId,
            val index: Int = 0,
            val selectedItemId: SelectedItemId? = null
        ) : Navigate() {
            internal constructor(event: MediaItemClicked) : this(
                event.tweetId,
                event.index,
                event.selectedItemId
            )

            override val type: NavigationEvent.Type = NavigationEvent.Type.NAVIGATE
        }

        abstract val type: NavigationEvent.Type
    }
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
