package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.data.ListRepository
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.LoadingResult
import com.freshdigitable.udonroad2.model.app.load
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEffectStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppAction1
import com.freshdigitable.udonroad2.model.app.navigation.AppEffect
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener
import com.freshdigitable.udonroad2.model.app.navigation.AppEventListener1
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.TimelineEffect
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

interface ListItemLoadableViewModel<Q : QueryType> :
    ListItemLoadableEventListener,
    ActivityEffectStream {
    val listState: LiveData<State>
    val timeline: Flow<PagingData<Any>>

    interface State {
        val isHeadingEnabled: Boolean
        val isHeadingVisible: Boolean
        val isPrepending: Boolean
    }
}

interface ListItemLoadableEventListener {
    val prependList: AppEventListener
    val scrollList: AppEventListener
    val stopScrollingList: AppEventListener1<Int>
    val heading: AppEventListener
    val listVisible: AppEventListener1<Boolean>
}

internal interface ListItemLoadableAction : ListItemLoadableEventListener {
    override val prependList: AppAction<TimelineEvent.SwipedToRefresh>
    override val scrollList: AppAction<TimelineEvent.ListScrolled.Started>
    override val stopScrollingList: AppAction1<Int, TimelineEvent.ListScrolled.Stopped>
    override val heading: AppAction<TimelineEvent.HeadingClicked>
    override val listVisible: AppAction1<Boolean, TimelineEvent.ListVisible>
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
    override val listVisible = eventDispatcher.toAction { it: Boolean ->
        TimelineEvent.ListVisible(owner, it)
    }
}

interface ListItemLoadableViewModelSource : ListItemLoadableEventListener, ActivityEffectStream {
    val pagedList: Flow<PagingData<Any>>
    val state: Flow<ListItemLoadableViewModel.State>

    /**
     * clear state or dispose some resources.
     *
     * expected to be called on androidx.lifecycle.ViewModel.onCleared(),
     * defined as plain (not suspended) function.
     */
    fun clear() {}
}

internal class ListItemLoadableViewStateImpl(
    private val owner: ListOwner<QueryType>,
    actions: ListItemLoadableAction,
    private val listRepository: ListRepository<QueryType, Any>,
    pagedListProvider: PagedListProvider<QueryType, Any>,
    private val appExecutor: AppExecutor,
) : ListItemLoadableViewModelSource,
    ListItemLoadableEventListener by actions,
    ActivityEffectStream {

    override val pagedList: Flow<PagingData<Any>> = pagedListProvider.getList(owner.query, owner.id)
    private val channel = Channel<AppEffect>()

    override val state: Flow<Snapshot> = stateSourceBuilder(
        init = Snapshot(),
        actions.scrollList.onEvent { s, _ -> s.copy(enableHeading = true) },
        actions.stopScrollingList.onEvent { s, e ->
            s.copy(
                firstVisibleItemPosition = e.firstVisibleItemPosition,
                enableHeading = false,
            )
        },
        actions.prependList.flatMapLatest {
            flow {
                emit(LoadingResult.Started)
                emit(listRepository.load { prependList(owner.query, owner.id) })
            }
        }.onEvent { state, result ->
            when (result) {
                is LoadingResult.Started -> state.copy(isPrepending = true)
                is LoadingResult.Loaded -> {
                    val items = result.value
                    val firstVisibleItemPosition = state.firstVisibleItemPosition + items.size
                    state.copy(
                        firstVisibleItemPosition = firstVisibleItemPosition,
                        isPrepending = false,
                    )
                }
                is LoadingResult.Failed -> state.copy(isPrepending = false)
            }
        },
        actions.listVisible.onEvent { s, e ->
            if (e.owner == owner) {
                s.copy(isHeadingVisible = e.isVisible)
            } else {
                if (e.isVisible) s.copy(isHeadingVisible = false) else s
            }
        },
        actions.heading.filter { it.owner == owner }.onEvent { s, _ ->
            if (s.firstVisibleItemPosition > 0) {
                val needsSkip = s.firstVisibleItemPosition >= 4
                channel.send(TimelineEffect.ToTopOfList(needsSkip))
            }
            s
        },
    )

    override val effect: Flow<AppEffect> = channel.receiveAsFlow()

    override fun clear() {
        channel.close()
        Timber.tag("ListItemLoadableVS").d("clear: $owner")
        appExecutor.launch {
            listRepository.clear(owner.id)
        }
    }

    internal data class Snapshot(
        val firstVisibleItemPosition: Int = RecyclerView.NO_POSITION,
        val enableHeading: Boolean = false,
        override val isPrepending: Boolean = false,
        override val isHeadingVisible: Boolean = true,
    ) : ListItemLoadableViewModel.State {
        override val isHeadingEnabled: Boolean
            get() = enableHeading || when (firstVisibleItemPosition) {
                RecyclerView.NO_POSITION -> false
                else -> firstVisibleItemPosition > 0
            }
    }
}
