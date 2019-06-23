package com.freshdigitable.udonroad2.timeline

import androidx.lifecycle.LiveData
import androidx.paging.PagedList

interface ListLoadable {
    val loading: LiveData<Boolean>
    fun onRefresh()
}

interface ListItemLoadable<T> : ListLoadable {
    fun getList(listOwner: ListOwner): LiveData<PagedList<T>>
}
