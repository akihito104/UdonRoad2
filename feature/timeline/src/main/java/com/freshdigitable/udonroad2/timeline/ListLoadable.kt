package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import kotlinx.coroutines.flow.Flow

interface ListLoadable {
    fun onRefresh()
}

interface ListItemLoadable<Q : QueryType, T : Any> : ListLoadable {
    val timeline: Flow<PagingData<T>>
}

abstract class ListItemLoadableViewModel<Q : QueryType, T : Any> : ListItemLoadable<Q, T>,
    ViewModel() {
    abstract val navigationEvent: Flow<NavigationEvent>
    abstract val feedbackMessage: Flow<FeedbackMessage>
}
