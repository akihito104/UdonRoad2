package com.freshdigitable.udonroad2.timeline

import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
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
}

sealed class TimelineEffect : AppEffect {
    data class ToTopOfList(val needsSkip: Boolean) : TimelineEffect()

    sealed class Navigate : AppEffect.Navigation, TimelineEffect() {
        data class Timeline(
            val owner: ListOwner<*>,
            override val type: AppEffect.Navigation.Type = AppEffect.Navigation.Type.NAVIGATE,
        ) : Navigate()

        data class Detail(
            val id: TweetId,
        ) : Navigate()

        data class UserInfo(val tweetUserItem: TweetUserItem) : Navigate()

        data class MediaViewer(
            val tweetId: TweetId,
            val index: Int = 0,
        ) : Navigate() {
            internal constructor(event: TimelineEvent.MediaItemClicked) :
                this(event.tweetId, event.index)
        }
    }
}

suspend fun ListOwnerGenerator.getTimelineEvent(
    queryType: QueryType,
    navType: AppEffect.Navigation.Type = AppEffect.Navigation.Type.NAVIGATE,
): TimelineEffect.Navigate.Timeline = TimelineEffect.Navigate.Timeline(generate(queryType), navType)
