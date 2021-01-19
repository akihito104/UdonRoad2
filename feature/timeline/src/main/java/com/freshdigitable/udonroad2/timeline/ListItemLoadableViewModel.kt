package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.ActivityEventStream
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx2.asFlow

abstract class ListItemLoadableViewModel<Q : QueryType, T : Any>(
    private val eventDispatcher: EventDispatcher,
    viewState: ListItemLoadableViewState = ListItemLoadableViewState.create(eventDispatcher),
    activityEventStream: ActivityEventStream = ActivityEventStream.EmptyStream,
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
}

interface ListItemLoadableActions {
    val scrollList: AppAction<out TimelineEvent.ListScrolled>

    companion object {
        fun create(eventDispatcher: EventDispatcher): ListItemLoadableActions =
            object : ListItemLoadableActions {
                override val scrollList: AppAction<out TimelineEvent.ListScrolled> =
                    eventDispatcher.toAction()
            }
    }
}

interface ListItemLoadableViewState {
    val isHeadingEnabled: Flow<Boolean>

    companion object {
        fun create(eventDispatcher: EventDispatcher): ListItemLoadableViewState = create(
            ListItemLoadableActions.create(eventDispatcher)
        )

        fun create(actions: ListItemLoadableActions): ListItemLoadableViewState =
            object : ListItemLoadableViewState {
                override val isHeadingEnabled: Flow<Boolean> = actions.scrollList.asFlow()
                    .onStart { emit(TimelineEvent.ListScrolled.Stopped(RecyclerView.NO_POSITION)) }
                    .mapLatest {
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
            }
    }
}
