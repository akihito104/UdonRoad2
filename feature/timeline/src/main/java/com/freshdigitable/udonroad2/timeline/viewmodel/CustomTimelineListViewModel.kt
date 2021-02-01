package com.freshdigitable.udonroad2.timeline.viewmodel

import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewState
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class CustomTimelineListViewModel(
    owner: ListOwner<QueryType.UserListMembership>,
    viewState: ListItemLoadableViewState,
    private val eventDispatcher: EventDispatcher,
) : ListItemLoadableViewModel<QueryType.UserListMembership>(owner, eventDispatcher, viewState),
    ListItemClickListener<CustomTimelineItem> {

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onBodyItemClicked(item: CustomTimelineItem) {
        eventDispatcher.postEvent(TimelineEvent.CustomTimelineClicked(item))
    }
}
