package com.freshdigitable.udonroad2.timeline.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.CustomTimelineListQueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import com.freshdigitable.udonroad2.timeline.getTimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

internal class CustomTimelineListViewModel(
    viewModelSource: CustomTimelineListItemLoadableViewState,
    userIconViewModelSource: UserIconViewModelSource,
) : ListItemLoadableViewModel<CustomTimelineListQueryType>,
    ListItemLoadableEventListener by viewModelSource,
    ListItemClickListener<CustomTimelineItem> by viewModelSource,
    UserIconClickListener by userIconViewModelSource,
    ActivityEventStream by viewModelSource, ViewModel() {
    override val listState: LiveData<ListItemLoadableViewModel.State> =
        viewModelSource.state.asLiveData(viewModelScope.coroutineContext)
    override val timeline: Flow<PagingData<Any>> =
        viewModelSource.pagedList.cachedIn(viewModelScope)
    override val navigationEvent: Flow<NavigationEvent> = merge(
        viewModelSource.navigationEvent,
        userIconViewModelSource.navEvent,
    )
}

internal class CustomTimelineListActions @Inject constructor(
    private val eventDispatcher: EventDispatcher,
) : ListItemClickListener<CustomTimelineItem> {
    internal val launchCustomTimeline: Flow<TimelineEvent.CustomTimelineClicked> =
        eventDispatcher.toActionFlow()

    override fun onBodyItemClicked(item: CustomTimelineItem) {
        eventDispatcher.postEvent(TimelineEvent.CustomTimelineClicked(item))
    }
}

internal class CustomTimelineListItemLoadableViewState(
    actions: CustomTimelineListActions,
    viewModelSource: ListItemLoadableViewModelSource,
    listOwner: ListOwnerGenerator,
) : ListItemLoadableViewModelSource by viewModelSource,
    ListItemClickListener<CustomTimelineItem> by actions {
    override val navigationEvent: Flow<NavigationEvent> = merge(
        viewModelSource.navigationEvent,
        actions.launchCustomTimeline.mapLatest {
            val queryType = QueryType.TweetQueryType.CustomTimeline(
                it.customTimeline.id,
                it.customTimeline.name
            )
            listOwner.getTimelineEvent(queryType, NavigationEvent.Type.NAVIGATE)
        }
    )
}
