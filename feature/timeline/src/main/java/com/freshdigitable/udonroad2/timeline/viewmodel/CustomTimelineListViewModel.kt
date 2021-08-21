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
import com.freshdigitable.udonroad2.model.QueryType.CustomTimelineList
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.getTimelineEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.timeline.ListItemClickListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableEventListener
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModel
import com.freshdigitable.udonroad2.timeline.ListItemLoadableViewModelSource
import com.freshdigitable.udonroad2.timeline.TimelineEvent
import com.freshdigitable.udonroad2.timeline.UserIconClickListener
import com.freshdigitable.udonroad2.timeline.UserIconViewModelSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

internal class CustomTimelineListViewModel(
    private val viewModelSource: CustomTimelineListItemLoadableViewState,
    userIconViewModelSource: UserIconViewModelSource,
) : ListItemLoadableViewModel<CustomTimelineList>,
    ListItemLoadableEventListener by viewModelSource,
    ListItemClickListener<CustomTimelineItem> by viewModelSource,
    UserIconClickListener by userIconViewModelSource,
    ActivityEffectStream,
    ViewModel() {
    override val listState: LiveData<ListItemLoadableViewModel.State> =
        viewModelSource.state.asLiveData(viewModelScope.coroutineContext)
    override val timeline: Flow<PagingData<Any>> =
        viewModelSource.pagedList.cachedIn(viewModelScope)
    override val effect: Flow<AppEffect> = merge(
        viewModelSource.effect,
        userIconViewModelSource.navEvent,
    )

    override fun onCleared() {
        super.onCleared()
        viewModelSource.clear()
    }
}

internal class CustomTimelineListActions @Inject constructor(
    eventDispatcher: EventDispatcher,
) : ListItemClickListener<CustomTimelineItem> {
    override val selectBodyItem = eventDispatcher.toAction { item: CustomTimelineItem ->
        TimelineEvent.CustomTimelineClicked(item)
    }
}

internal class CustomTimelineListItemLoadableViewState(
    actions: CustomTimelineListActions,
    viewModelSource: ListItemLoadableViewModelSource,
    listOwner: ListOwnerGenerator,
) : ListItemLoadableViewModelSource by viewModelSource,
    ListItemClickListener<CustomTimelineItem> by actions {
    override val effect: Flow<AppEffect> = merge(
        viewModelSource.effect,
        actions.selectBodyItem.mapLatest {
            val queryType = QueryType.Tweet.CustomTimeline(
                it.customTimeline.id,
                it.customTimeline.name
            )
            listOwner.getTimelineEvent(queryType, AppEffect.Navigation.Type.NAVIGATE)
        }
    )
}
