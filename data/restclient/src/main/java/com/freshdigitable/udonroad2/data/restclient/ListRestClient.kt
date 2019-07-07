package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.ListQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Paging

private const val FETCH_COUNT = 50

interface ListRestClient<Q : ListQuery, E> {
    var query: Q

    suspend fun fetchInit(): List<E> = withContext(Dispatchers.IO) {
        fetchTimeline()
    }

    suspend fun fetchAtTop(cursorId: Long = 1): List<E> = withContext(Dispatchers.IO) {
        val paging = Paging(1, FETCH_COUNT, cursorId)
        fetchTimeline(paging)
    }

    suspend fun fetchAtBottom(cursorId: Long = 1): List<E> = withContext(Dispatchers.IO) {
        val paging = Paging(1, FETCH_COUNT, 1, cursorId)
        fetchTimeline(paging)
    }

    suspend fun fetchTimeline(paging: Paging? = null): List<E>
}
