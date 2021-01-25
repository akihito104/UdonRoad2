package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

abstract class ListItemLoadableViewModel<Q : QueryType>(
    private val owner: ListOwner<Q>,
    private val eventDispatcher: EventDispatcher,
    private val viewState: ListItemLoadableViewState,
    activityEventStream: ActivityEventStream = viewState,
) : ViewModel(), ActivityEventStream by activityEventStream {
    val isHeadingEnabled: Flow<Boolean> = viewState.isHeadingEnabled
    val timeline: Flow<PagingData<Any>> = viewState.pagedList.cachedIn(viewModelScope)

    fun onRefresh() {
        eventDispatcher.postEvent(TimelineEvent.SwipedToRefresh)
    }

    internal fun onListScrollStarted() {
        eventDispatcher.postEvent(TimelineEvent.ListScrolled.Started)
    }

    internal fun onListScrollStopped(firstVisibleItemPosition: Int) {
        eventDispatcher.postEvent(TimelineEvent.ListScrolled.Stopped(firstVisibleItemPosition))
    }

    internal fun onHeadingClicked() {
        eventDispatcher.postEvent(TimelineEvent.HeadingClicked(owner))
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            viewState.clear()
        }
    }
}

interface ListItemLoadableActions {
    val scrollList: AppAction<out TimelineEvent.ListScrolled>
    val heading: AppAction<TimelineEvent.HeadingClicked>
    val prependList: AppAction<TimelineEvent.SwipedToRefresh>

    companion object {
        fun create(eventDispatcher: EventDispatcher): ListItemLoadableActions =
            ListItemLoadableActionsImpl(eventDispatcher)
    }
}

interface ListItemLoadableViewState : ActivityEventStream {
    val pagedList: Flow<PagingData<Any>>
    val isHeadingEnabled: Flow<Boolean>

    suspend fun clear() {}
}

internal class ListItemLoadableActionsImpl @Inject constructor(
    eventDispatcher: EventDispatcher
) : ListItemLoadableActions {
    override val scrollList: AppAction<out TimelineEvent.ListScrolled> = eventDispatcher.toAction()
    override val heading: AppAction<TimelineEvent.HeadingClicked> = eventDispatcher.toAction()
    override val prependList: AppAction<TimelineEvent.SwipedToRefresh> = eventDispatcher.toAction()
}

internal class ListItemLoadableViewStateImpl(
    private val owner: ListOwner<QueryType>,
    actions: ListItemLoadableActions,
    private val listRepository: ListRepository<QueryType, *>,
    pagedListProvider: PagedListProvider<QueryType, Any>,
) : ListItemLoadableViewState, ActivityEventStream by ActivityEventStream.EmptyStream {

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override val pagedList: Flow<PagingData<Any>> = pagedListProvider.getList(owner.query, owner.id)

    private val snapshot: StateFlow<Snapshot> = merge(
        actions.scrollList.asFlow(),
        actions.prependList.asFlow(),
    ).scan(Snapshot()) { acc, value ->
        when (value) {
            is TimelineEvent.ListScrolled.Started -> {
                Snapshot(
                    firstVisibleItemPosition = acc.firstVisibleItemPosition,
                    isHeadingEnabled = true
                )
            }
            is TimelineEvent.ListScrolled.Stopped -> {
                Snapshot(
                    firstVisibleItemPosition = value.firstVisibleItemPosition,
                    isHeadingEnabled = isHeadingEnabled(value.firstVisibleItemPosition)
                )
            }
            is TimelineEvent.SwipedToRefresh -> {
                val items = listRepository.prependList(owner.query, owner.id)
                val firstVisibleItemPosition = acc.firstVisibleItemPosition + items.size
                Snapshot(
                    firstVisibleItemPosition = firstVisibleItemPosition,
                    isHeadingEnabled = isHeadingEnabled(firstVisibleItemPosition)
                )
            }
            else -> throw IllegalStateException()
        }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Snapshot())

    override val isHeadingEnabled: Flow<Boolean> = snapshot.mapLatest { it.isHeadingEnabled }
        .distinctUntilChanged()
    override val navigationEvent: Flow<NavigationEvent> = actions.heading.asFlow()
        .filter { snapshot.value.firstVisibleItemPosition > 0 }
        .mapLatest {
            val needsSkip = snapshot.value.firstVisibleItemPosition >= 4
            TimelineEvent.Navigate.ToTopOfList(needsSkip)
        }

    override suspend fun clear() {
        scope.cancel()
        listRepository.clear(owner.id)
    }

    private data class Snapshot(
        val firstVisibleItemPosition: Int = RecyclerView.NO_POSITION,
        val isHeadingEnabled: Boolean = false
    )

    companion object {
        private fun isHeadingEnabled(firstVisibleItemPosition: Int): Boolean =
            when (firstVisibleItemPosition) {
                RecyclerView.NO_POSITION -> false
                else -> firstVisibleItemPosition > 0
            }
    }
}
