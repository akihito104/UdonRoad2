package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem

interface ListDao<E, I> {
    fun getList(owner: String): DataSource.Factory<Int, I>
    suspend fun addEntities(tweet: List<E>, owner: String? = null)
    suspend fun clean(owner: String)
}

class TweetListDao(
    private val dao: TweetDao
) : ListDao<TweetEntity, TweetListItem> {
    override fun getList(owner: String): DataSource.Factory<Int, TweetListItem> {
        return dao.getTimeline(owner).map { it as TweetListItem }
    }

    override suspend fun addEntities(tweet: List<TweetEntity>, owner: String?) {
        dao.addTweets(tweet, owner)
    }

    override suspend fun clean(owner: String) {
        dao.clear(owner)
    }
}
