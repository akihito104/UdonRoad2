package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadable
import com.freshdigitable.udonroad2.timeline.TimelineEvent

class CustomTimelineListViewModel(
    private val owner: ListOwner<QueryType.UserListMembership>,
    private val repository: ListRepository<QueryType.UserListMembership>,
    private val eventDispatcher: EventDispatcher,
    pagedListProvider: PagedListProvider<QueryType.UserListMembership, CustomTimelineItem>
) : ListItemLoadable<QueryType.UserListMembership, CustomTimelineItem>,
    ListItemClickListener<CustomTimelineItem>, ViewModel() {
    override val loading: LiveData<Boolean>
        get() = repository.loading

    override fun onRefresh() {
        val q = if (timeline.value?.isNotEmpty() == true) {
            ListQuery(owner.query, PageOption.OnHead())
        } else {
            ListQuery(owner.query, PageOption.OnInit)
        }
        repository.loadList(q, owner.id)
    }

    override val timeline: LiveData<PagedList<CustomTimelineItem>> =
        pagedListProvider.getList(owner.query, owner.id)

    override fun onUserIconClicked(user: TweetUserItem) {
        eventDispatcher.postEvent(TimelineEvent.UserIconClicked(user))
    }

    override fun onBodyItemClicked(item: CustomTimelineItem) {
        eventDispatcher.postEvent(TimelineEvent.CustomTimelineClicked(item))
    }

    override fun onCleared() {
        super.onCleared()
        repository.clear(owner.id)
    }
}
