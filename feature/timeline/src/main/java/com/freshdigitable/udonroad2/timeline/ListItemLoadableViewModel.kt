package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toActionFlow
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

interface ListItemLoadableViewModel<Q : QueryType> :
    ListItemLoadableEventListener,
    ActivityEventStream {
    val listState: LiveData<State>
    val timeline: Flow<PagingData<Any>>

    interface State {
        val isHeadingEnabled: Boolean
    }
}

interface ListItemLoadableEventListener {
    fun onRefresh()
    fun onListScrollStarted()
    fun onListScrollStopped(firstVisibleItemPosition: Int)
    fun onHeadingClicked()
}

internal interface ListItemLoadableAction : ListItemLoadableEventListener {
    val scrollList: Flow<TimelineEvent.ListScrolled>
    val heading: Flow<TimelineEvent.HeadingClicked>
    val prependList: Flow<TimelineEvent.SwipedToRefresh>
}

internal class ListItemLoadableActions @Inject constructor(
    private val owner: ListOwner<*>,
    private val eventDispatcher: EventDispatcher,
) : ListItemLoadableAction {
    override fun onRefresh() {
        eventDispatcher.postEvent(TimelineEvent.SwipedToRefresh)
    }

    override fun onListScrollStarted() {
        eventDispatcher.postEvent(TimelineEvent.ListScrolled.Started)
    }

    override fun onListScrollStopped(firstVisibleItemPosition: Int) {
        eventDispatcher.postEvent(TimelineEvent.ListScrolled.Stopped(firstVisibleItemPosition))
    }

    override fun onHeadingClicked() {
        eventDispatcher.postEvent(TimelineEvent.HeadingClicked(owner))
    }

    override val scrollList: Flow<TimelineEvent.ListScrolled> = eventDispatcher.toActionFlow()
    override val heading: Flow<TimelineEvent.HeadingClicked> =
        eventDispatcher.toActionFlow { it.owner == owner }
    override val prependList: Flow<TimelineEvent.SwipedToRefresh> = eventDispatcher.toActionFlow()
}

interface ListItemLoadableViewModelSource : ListItemLoadableEventListener, ActivityEventStream {
    val pagedList: Flow<PagingData<Any>>
    val state: Flow<ListItemLoadableViewModel.State>

    suspend fun clear() {}
}

internal class ListItemLoadableViewStateImpl(
    private val owner: ListOwner<QueryType>,
    actions: ListItemLoadableAction,
    private val listRepository: ListRepository<QueryType, Any>,
    pagedListProvider: PagedListProvider<QueryType, Any>,
) : ListItemLoadableViewModelSource,
    ListItemLoadableEventListener by actions,
    ActivityEventStream by ActivityEventStream.EmptyStream {

    override val pagedList: Flow<PagingData<Any>> = pagedListProvider.getList(owner.query, owner.id)
    private val channel = Channel<NavigationEvent>()

    override val state: Flow<Snapshot> = stateSourceBuilder(
        init = Snapshot(),
        actions.scrollList.onEvent { s, e ->
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
        actions.prependList.onEvent { state, _ ->
            val items = listRepository.prependList(owner.query, owner.id)
            val firstVisibleItemPosition = state.firstVisibleItemPosition + items.size
            Snapshot(
                firstVisibleItemPosition = firstVisibleItemPosition,
                isHeadingEnabled = isHeadingEnabled(firstVisibleItemPosition)
            )
        },
        actions.heading.onEvent { s, _ ->
            if (s.firstVisibleItemPosition > 0) {
                val needsSkip = s.firstVisibleItemPosition >= 4
                channel.send(TimelineEvent.Navigate.ToTopOfList(needsSkip))
            }
            s
        },
    )

    override val navigationEvent: Flow<NavigationEvent> = channel.receiveAsFlow()

    override suspend fun clear() {
        channel.close()
        listRepository.clear(owner.id)
    }

    internal data class Snapshot(
        val firstVisibleItemPosition: Int = RecyclerView.NO_POSITION,
        override val isHeadingEnabled: Boolean = false,
    ) : ListItemLoadableViewModel.State

    companion object {
        private fun isHeadingEnabled(firstVisibleItemPosition: Int): Boolean =
            when (firstVisibleItemPosition) {
                RecyclerView.NO_POSITION -> false
                else -> firstVisibleItemPosition > 0
            }
    }
}
