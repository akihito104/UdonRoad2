package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.model.QueryType

interface ListLoadable {
    val loading: LiveData<Boolean>
    fun onRefresh()
}

interface ListItemLoadable<Q : QueryType, T> : ListLoadable {
    val timeline: LiveData<PagedList<T>>
}
