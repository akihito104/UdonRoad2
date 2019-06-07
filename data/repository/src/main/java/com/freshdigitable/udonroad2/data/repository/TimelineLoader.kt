package com.freshdigitable.udonroad2.data.repository

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.freshdigitable.udonroad2.data.restclient.TweetListRestClient
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem

interface TimelineRepository {
    val loading: LiveData<Boolean>

    fun getTimeline(owner: String, query: ListQuery): LiveData<PagedList<TweetListItem>>
    fun loadAtFront()
    fun clear()
}

interface TimelineFetcher<T> {
    val fetchOnZeroItems: suspend T.() -> List<TweetEntity>
    val fetchOnBottom: (TweetListItem) -> (suspend T.() -> List<TweetEntity>)
    val fetchOnTop: (TweetListItem) -> (suspend T.() -> List<TweetEntity>)
}

class TweetTimelineFetcher : TimelineFetcher<TweetListRestClient<ListQuery>> {
    override val fetchOnZeroItems: suspend TweetListRestClient<ListQuery>.() -> List<TweetEntity>
        get() = { fetchInit() }
    override val fetchOnBottom: (TweetListItem) -> suspend TweetListRestClient<ListQuery>.() -> List<TweetEntity>
        get() = { item -> { fetchAtBottom(item.originalId - 1) } }
    override val fetchOnTop: (TweetListItem) -> suspend TweetListRestClient<ListQuery>.() -> List<TweetEntity>
        get() = { item -> { fetchAtTop(item.originalId + 1) } }
}
