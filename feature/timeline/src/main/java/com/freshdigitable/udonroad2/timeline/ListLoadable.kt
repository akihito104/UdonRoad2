package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.NavigationEvent
import kotlinx.coroutines.flow.Flow

interface ListLoadable {
    val loading: LiveData<Boolean>
    fun onRefresh()
}

interface ListItemLoadable<Q : QueryType, T> : ListLoadable {
    val timeline: LiveData<PagedList<T>>
}

abstract class ListItemLoadableViewModel<Q : QueryType, T> : ListItemLoadable<Q, T>, ViewModel() {
    internal abstract val navigationEvent: Flow<NavigationEvent>
    internal abstract val feedbackMessage: Flow<FeedbackMessage>
}
