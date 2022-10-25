package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import java.io.Serializable

sealed class TimelineEvent : AppEvent {
    data class Setup(val savedState: Serializable? = null) : TimelineEvent()

    data class UserIconClicked(val user: TweetUserItem) : TimelineEvent()

    data class RetweetUserClicked(val user: TweetUserItem) : TimelineEvent()

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

    data class CustomTimelineClicked(val customTimeline: CustomTimelineItem) : TimelineEvent()

    data class MediaItemClicked(
        val tweetId: TweetId,
        val index: Int = 0,
    ) : TimelineEvent()

    sealed class ListScrolled : TimelineEvent() {
        object Started : ListScrolled()
        data class Stopped(val firstVisibleItemPosition: Int) : ListScrolled()
    }

    data class HeadingClicked(val owner: ListOwner<*>) : TimelineEvent()
    object SwipedToRefresh : TimelineEvent()
    data class ListVisible(val owner: ListOwner<*>, val isVisible: Boolean) : TimelineEvent()
}

internal fun TimelineEffect.Navigate.MediaViewer.Companion.create(
    event: TimelineEvent.MediaItemClicked,
): TimelineEffect.Navigate.MediaViewer =
    TimelineEffect.Navigate.MediaViewer(event.tweetId, event.index)
