package com.freshdigitable.udonroad2.data.repository

import com.freshdigitable.udonroad2.data.restclient.ListRestClient
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.model.UserListItem

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

class UserListFetcher :
    ListFetcher<ListQuery, User, ListRestClient<ListQuery, User>, UserListItem> {

    override val fetchOnBottom: (UserListItem) -> suspend ListRestClient<ListQuery, User>.() -> List<User>
        get() = { _ -> { fetchAtBottom(1) } }
    override val fetchOnTop: (UserListItem) -> suspend ListRestClient<ListQuery, User>.() -> List<User>
        get() = { _ -> { fetchAtTop(1) } }
}
