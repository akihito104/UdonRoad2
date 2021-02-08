package com.freshdigitable.udonroad2.timeline.viewmodel

import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewState
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.UserIconClickedAction
import com.freshdigitable.udonroad2.timeline.UserIconClickedNavigation
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class CustomTimelineListViewModel(
    owner: ListOwner<QueryType.CustomTimelineListQueryType>,
    viewState: ListItemLoadableViewState,
    private val eventDispatcher: EventDispatcher,
) : ListItemLoadableViewModel<QueryType.CustomTimelineListQueryType>(
    owner,
    eventDispatcher,
    viewState
),
    ListItemClickListener<CustomTimelineItem> {

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onBodyItemClicked(item: CustomTimelineItem) {
        eventDispatcher.postEvent(TimelineEvent.CustomTimelineClicked(item))
    }
}

internal class CustomTimelineListActions @Inject constructor(
    eventDispatcher: EventDispatcher
) : UserIconClickedAction by UserIconClickedAction.create(eventDispatcher) {
    internal val launchCustomTimeline: AppAction<TimelineEvent.CustomTimelineClicked> =
        eventDispatcher.toAction()
}

internal class CustomTimelineListItemLoadableViewState(
    actions: CustomTimelineListActions,
    viewState: ListItemLoadableViewState,
    listOwner: ListOwnerGenerator,
) : ListItemLoadableViewState by viewState {
    private val userInfoNavigator = UserIconClickedNavigation.create(actions)
    override val navigationEvent: Flow<NavigationEvent> = merge(
        viewState.navigationEvent,
        userInfoNavigator.navEvent,
        actions.launchCustomTimeline.asFlow().mapLatest {
            val queryType = QueryType.TweetQueryType.CustomTimeline(
                it.customTimeline.id,
                it.customTimeline.name
            )
            listOwner.getTimelineEvent(queryType, NavigationEvent.Type.NAVIGATE)
        }
    )
}
