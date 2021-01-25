package com.freshdigitable.udonroad2.timeline.viewmodel

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserListItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewState
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import timber.log.Timber

class UserListViewModel(
    owner: ListOwner<QueryType.UserQueryType>,
    private val eventDispatcher: EventDispatcher,
    viewState: ListItemLoadableViewState,
) : ListItemLoadableViewModel<QueryType.UserQueryType>(owner, eventDispatcher, viewState),
    ListItemClickListener<UserListItem> {
    override fun onBodyItemClicked(item: UserListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.id}")
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(item))
    }

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }
}
