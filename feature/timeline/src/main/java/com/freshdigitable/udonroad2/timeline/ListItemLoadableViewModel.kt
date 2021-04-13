package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppAction1
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import com.freshdigitable.udonroad2.model.app.navigation.toAction
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
    val prependList: AppEventListener
    val scrollList: AppEventListener
    val stopScrollingList: AppEventListener1<Int>
    val heading: AppEventListener
}

internal interface ListItemLoadableAction : ListItemLoadableEventListener {
    override val prependList: AppAction<TimelineEvent.SwipedToRefresh>
    override val scrollList: AppAction<TimelineEvent.ListScrolled.Started>
    override val stopScrollingList: AppAction1<Int, TimelineEvent.ListScrolled.Stopped>
    override val heading: AppAction<TimelineEvent.HeadingClicked>
}

internal class ListItemLoadableActions @Inject constructor(
    owner: ListOwner<*>,
    eventDispatcher: EventDispatcher,
) : ListItemLoadableAction {
    override val prependList = eventDispatcher.toAction(TimelineEvent.SwipedToRefresh)
    override val scrollList = eventDispatcher.toAction(TimelineEvent.ListScrolled.Started)
    override val stopScrollingList = eventDispatcher.toAction { index: Int ->
        TimelineEvent.ListScrolled.Stopped(index)
    }
    override val heading: AppAction<TimelineEvent.HeadingClicked> =
        eventDispatcher.toAction(TimelineEvent.HeadingClicked(owner))
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
        actions.scrollList.onEvent { s, _ -> s.copy(enableHeading = true) },
        actions.stopScrollingList.onEvent { s, e ->
            s.copy(
                firstVisibleItemPosition = e.firstVisibleItemPosition,
                enableHeading = false,
            )
        },
        actions.prependList.onEvent { state, _ ->
            val items = listRepository.prependList(owner.query, owner.id)
            val firstVisibleItemPosition = state.firstVisibleItemPosition + items.size
            Snapshot(
                firstVisibleItemPosition = firstVisibleItemPosition,
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
        val enableHeading: Boolean = false,
    ) : ListItemLoadableViewModel.State {
        override val isHeadingEnabled: Boolean
            get() = enableHeading || when (firstVisibleItemPosition) {
                RecyclerView.NO_POSITION -> false
                else -> firstVisibleItemPosition > 0
            }
    }
}
