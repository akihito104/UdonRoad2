package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.rx2.asFlow

abstract class ListItemLoadableViewModel<Q : QueryType, T : Any>(
    private val owner: ListOwner<Q>,
    private val eventDispatcher: EventDispatcher,
    viewState: ListItemLoadableViewState = ListItemLoadableViewState.create(eventDispatcher),
    activityEventStream: ActivityEventStream = viewState,
) : ViewModel(), ActivityEventStream by activityEventStream {
    val isHeadingEnabled: Flow<Boolean> = viewState.isHeadingEnabled
    abstract val timeline: Flow<PagingData<T>>

    abstract fun onRefresh()

    internal fun onListScrollStarted() {
        eventDispatcher.postEvent(TimelineEvent.ListScrolled.Started)
    }

    internal fun onListScrollStopped(firstVisibleItemPosition: Int) {
        eventDispatcher.postEvent(TimelineEvent.ListScrolled.Stopped(firstVisibleItemPosition))
    }

    internal fun onHeadingClicked() {
        eventDispatcher.postEvent(TimelineEvent.HeadingClicked(owner))
    }
}

interface ListItemLoadableActions {
    val scrollList: AppAction<out TimelineEvent.ListScrolled>
    val heading: AppAction<TimelineEvent.HeadingClicked>

    companion object {
        fun create(eventDispatcher: EventDispatcher): ListItemLoadableActions =
            object : ListItemLoadableActions {
                override val scrollList: AppAction<out TimelineEvent.ListScrolled> =
                    eventDispatcher.toAction()
                override val heading: AppAction<TimelineEvent.HeadingClicked> =
                    eventDispatcher.toAction()
            }
    }
}

interface ListItemLoadableViewState : ActivityEventStream {
    val isHeadingEnabled: Flow<Boolean>

    companion object {
        fun create(
            eventDispatcher: EventDispatcher,
            scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        ): ListItemLoadableViewState = create(
            ListItemLoadableActions.create(eventDispatcher), scope
        )

        fun create(
            actions: ListItemLoadableActions,
            scope: CoroutineScope
        ): ListItemLoadableViewState {
            val scrollListFlow: SharedFlow<TimelineEvent.ListScrolled> = actions.scrollList.asFlow()
                .onStart { emit(TimelineEvent.ListScrolled.Stopped(RecyclerView.NO_POSITION)) }
                .shareIn(scope, SharingStarted.Eagerly, 1)
            return object : ListItemLoadableViewState,
                ActivityEventStream by ActivityEventStream.EmptyStream {
                private val firstVisibleItemPosition =
                    scrollListFlow.filterIsInstance<TimelineEvent.ListScrolled.Stopped>()
                        .mapLatest { it.firstVisibleItemPosition }
                        .distinctUntilChanged()
                        .stateIn(scope, SharingStarted.Eagerly, RecyclerView.NO_POSITION)
                override val isHeadingEnabled: Flow<Boolean> = scrollListFlow.mapLatest {
                    when (it) {
                        is TimelineEvent.ListScrolled.Started -> true
                        is TimelineEvent.ListScrolled.Stopped -> {
                            if (it.firstVisibleItemPosition == RecyclerView.NO_POSITION) {
                                false
                            } else {
                                it.firstVisibleItemPosition != 0
                            }
                        }
                    }
                }
                    .distinctUntilChanged()
                override val navigationEvent: Flow<NavigationEvent> =
                    actions.heading.asFlow().transformLatest {
                        if (firstVisibleItemPosition.value > 0) {
                            emit(TimelineEvent.Navigate.ToTopOfList(needsSkip = firstVisibleItemPosition.value >= 4))
                        }
                    }
            }
        }
    }
}
