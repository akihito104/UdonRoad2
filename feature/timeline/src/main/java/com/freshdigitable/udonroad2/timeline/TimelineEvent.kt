package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import java.io.Serializable

sealed class TimelineEvent : AppEvent {
    data class Setup(val savedState: Serializable? = null) : TimelineEvent()

    object Init : TimelineEvent()

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
        val selectedItemId: SelectedItemId? = null
    ) : TimelineEvent()

    sealed class ListScrolled : TimelineEvent() {
        object Started : ListScrolled()
        data class Stopped(val firstVisibleItemPosition: Int) : ListScrolled()
    }

    data class HeadingClicked(val owner: ListOwner<*>) : TimelineEvent()
    object SwipedToRefresh : TimelineEvent()

    sealed class Navigate : TimelineEvent(), NavigationEvent {
        data class Timeline(
            val owner: ListOwner<*>,
            override val type: NavigationEvent.Type = NavigationEvent.Type.NAVIGATE
        ) : Navigate()

        data class ToTopOfList(val needsSkip: Boolean) : Navigate() {
            override val type: NavigationEvent.Type
                get() = throw UnsupportedOperationException()
        }

        data class Detail(
            val id: TweetId,
            override val type: NavigationEvent.Type = NavigationEvent.Type.NAVIGATE
        ) : Navigate()

        data class UserInfo(val tweetUserItem: TweetUserItem) : Navigate() {
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

suspend fun ListOwnerGenerator.getTimelineEvent(
    queryType: QueryType,
    navType: NavigationEvent.Type
): TimelineEvent.Navigate.Timeline = TimelineEvent.Navigate.Timeline(generate(queryType), navType)
