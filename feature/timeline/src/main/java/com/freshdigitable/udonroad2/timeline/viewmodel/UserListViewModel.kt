package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.UserListItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class UserListViewModel(
    private val eventDispatcher: EventDispatcher,
    viewModelSource: ListItemLoadableViewModelSource,
    userIconViewModelSource: UserIconViewModelSource,
) : ListItemLoadableViewModel<QueryType.UserQueryType>,
    ListItemLoadableEventListener by viewModelSource,
    ListItemClickListener<UserListItem>, UserIconClickListener by userIconViewModelSource,
    ActivityEventStream by viewModelSource, ViewModel() {

    override val listState: LiveData<ListItemLoadableViewModel.State> =
        viewModelSource.state.asLiveData(viewModelScope.coroutineContext)
    override val timeline: Flow<PagingData<Any>> =
        viewModelSource.pagedList.cachedIn(viewModelScope)

    override fun onBodyItemClicked(item: UserListItem) {
        Timber.tag("TimelineViewModel").d("onBodyItemClicked: ${item.id}")
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(item))
    }
}
