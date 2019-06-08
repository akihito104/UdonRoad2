package com.freshdigitable.udonroad2.data.repository

import com.freshdigitable.udonroad2.data.restclient.ListRestClient
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem

interface ListFetcher<Q : ListQuery, E, RC : ListRestClient<Q, E>, I> {
    val fetchOnZeroItems: suspend RC.() -> List<E>
        get() = { fetchInit() }
    val fetchOnBottom: (I) -> (suspend RC.() -> List<E>)
    val fetchOnTop: (I) -> (suspend RC.() -> List<E>)
}

class TweetTimelineFetcher :
    ListFetcher<ListQuery, TweetEntity, ListRestClient<ListQuery, TweetEntity>, TweetListItem> {

    override val fetchOnBottom: (TweetListItem) -> suspend ListRestClient<ListQuery, TweetEntity>.() -> List<TweetEntity>
        get() = { item -> { fetchAtBottom(item.originalId - 1) } }
    override val fetchOnTop: (TweetListItem) -> suspend ListRestClient<ListQuery, TweetEntity>.() -> List<TweetEntity>
        get() = { item -> { fetchAtTop(item.originalId + 1) } }
}
