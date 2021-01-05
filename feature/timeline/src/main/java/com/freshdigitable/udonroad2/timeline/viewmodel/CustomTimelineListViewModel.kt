package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class CustomTimelineListViewModel(
    private val owner: ListOwner<QueryType.UserListMembership>,
    private val repository: ListRepository<QueryType.UserListMembership>,
    private val eventDispatcher: EventDispatcher,
    private val pagedListProvider: PagedListProvider<QueryType.UserListMembership, CustomTimelineItem>
) : ListItemLoadableViewModel<QueryType.UserListMembership, CustomTimelineItem>(),
    ListItemClickListener<CustomTimelineItem> {
    override val loading: LiveData<Boolean> = repository.loading

    override fun onRefresh() {
        viewModelScope.launch {
            repository.prependList(owner.query, owner.id)
        }
    }

    override val timeline: Flow<PagingData<CustomTimelineItem>> =
        pagedListProvider.getList(owner.query, owner.id)

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onBodyItemClicked(item: CustomTimelineItem) {
        eventDispatcher.postEvent(TimelineEvent.CustomTimelineClicked(item))
    }

    override fun onCleared() {
        super.onCleared()
        pagedListProvider.clear()
        viewModelScope.launch {
            repository.clear(owner.id)
        }
    }

    override val navigationEvent: Flow<NavigationEvent> = emptyFlow()
    override val feedbackMessage: Flow<FeedbackMessage> = emptyFlow()
}
