package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.ListQuery
import twitter4j.Paging

private const val FETCH_COUNT = 50

interface ListRestClient<Q : ListQuery, E> {
    var query: Q

    suspend fun fetchInit(): List<E> {
        return fetchTimeline()
    }

    suspend fun fetchAtTop(cursorId: Long = 1): List<E> {
        val paging = Paging(1, FETCH_COUNT, cursorId)
        return fetchTimeline(paging)
    }

    suspend fun fetchAtBottom(cursorId: Long = 1): List<E> {
        val paging = Paging(1, FETCH_COUNT, 1, cursorId)
        return fetchTimeline(paging)
    }

    suspend fun fetchTimeline(paging: Paging? = null): List<E>
}
