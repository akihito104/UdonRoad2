package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserListItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class UserListViewModel(
    private val owner: ListOwner<QueryType.UserQueryType>,
    private val eventDispatcher: EventDispatcher,
    private val repository: ListRepository<QueryType.UserQueryType>,
    private val pagedListProvider: PagedListProvider<QueryType.UserQueryType, UserListItem>
) : ListItemLoadableViewModel<QueryType.UserQueryType, UserListItem>(),
    ListItemClickListener<UserListItem> {

    override val timeline: LiveData<PagedList<UserListItem>> =
        pagedListProvider.getList(owner.query, owner.id)

    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        viewModelScope.launch {
            repository.prependList(owner.query, owner.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pagedListProvider.clear()
        viewModelScope.launch {
            repository.clear(owner.id)
        }
    }

    override fun onBodyItemClicked(item: UserListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.id}")
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(item))
    }

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override val navigationEvent: Flow<NavigationEvent> = emptyFlow()
    override val feedbackMessage: Flow<FeedbackMessage> = emptyFlow()
}
