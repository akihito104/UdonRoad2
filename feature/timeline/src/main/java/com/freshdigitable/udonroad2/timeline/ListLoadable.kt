package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.model.ListQuery

interface ListLoadable {
    val loading: LiveData<Boolean>
    fun onRefresh()
}

interface ListItemLoadable<Q : ListQuery, T> : ListLoadable {
    val timeline: LiveData<PagedList<T>>
}
