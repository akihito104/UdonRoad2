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
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

abstract class ListItemLoadableViewModel<Q : QueryType>(
    private val owner: ListOwner<Q>,
    private val eventDispatcher: EventDispatcher,
    private val viewState: ListItemLoadableViewState,
    activityEventStream: ActivityEventStream = viewState,
) : ViewModel(), ActivityEventStream by activityEventStream {
    val isHeadingEnabled: Flow<Boolean> =
        viewState.state.mapLatest { it.isHeadingEnabled }.distinctUntilChanged()
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
    val state: Flow<State>

    suspend fun clear() {}

    interface State {
        val isHeadingEnabled: Boolean
    }
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
    private val listRepository: ListRepository<QueryType, Any>,
    pagedListProvider: PagedListProvider<QueryType, Any>,
) : ListItemLoadableViewState, ActivityEventStream by ActivityEventStream.EmptyStream {

    override val pagedList: Flow<PagingData<Any>> = pagedListProvider.getList(owner.query, owner.id)
    private val channel = Channel<NavigationEvent>()

    override val state: Flow<Snapshot> = stateSourceBuilder(
        init = Snapshot(),
        actions.scrollList.asFlow().onEvent { s, e ->
            when (e) {
                is TimelineEvent.ListScrolled.Started -> {
                    Snapshot(
                        firstVisibleItemPosition = s.firstVisibleItemPosition,
                        isHeadingEnabled = true
                    )
                }
                is TimelineEvent.ListScrolled.Stopped -> {
                    Snapshot(
                        firstVisibleItemPosition = e.firstVisibleItemPosition,
                        isHeadingEnabled = isHeadingEnabled(e.firstVisibleItemPosition)
                    )
                }
            }
        },
        actions.prependList.asFlow().onEvent { state, _ ->
            val items = listRepository.prependList(owner.query, owner.id)
            val firstVisibleItemPosition = state.firstVisibleItemPosition + items.size
            Snapshot(
                firstVisibleItemPosition = firstVisibleItemPosition,
                isHeadingEnabled = isHeadingEnabled(firstVisibleItemPosition)
            )
        },
        actions.heading.asFlow().onEvent { s, _ ->
            if (s.firstVisibleItemPosition > 0) {
                val needsSkip = s.firstVisibleItemPosition >= 4
                channel.send(TimelineEvent.Navigate.ToTopOfList(needsSkip))
            }
            s
        },
    )
        .distinctUntilChanged()

    override val navigationEvent: Flow<NavigationEvent> = channel.receiveAsFlow()

    override suspend fun clear() {
        channel.close()
        listRepository.clear(owner.id)
    }

    internal data class Snapshot(
        val firstVisibleItemPosition: Int = RecyclerView.NO_POSITION,
        override val isHeadingEnabled: Boolean = false,
    ) : ListItemLoadableViewState.State

    companion object {
        private fun isHeadingEnabled(firstVisibleItemPosition: Int): Boolean =
            when (firstVisibleItemPosition) {
                RecyclerView.NO_POSITION -> false
                else -> firstVisibleItemPosition > 0
            }
    }
}
