package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserListItem

class TweetListDao(
    private val dao: TweetDao
) : LocalListDataSource<QueryType.TweetQueryType, TweetEntity>,
    PagedListProvider.DataSourceFactory<TweetListItem> {
    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, TweetListItem> {
        return dao.getTimeline(owner).map { it as TweetListItem }
    }

    override suspend fun putList(
        entities: List<TweetEntity>,
        query: ListQuery<QueryType.TweetQueryType>?,
        owner: ListId?
    ) {
        dao.addTweets(entities, owner)
    }

    override suspend fun clean(owner: ListId) {
        dao.clear(owner)
    }
}

class UserListDao(
    private val dao: UserDao
) : LocalListDataSource<QueryType.UserQueryType, UserEntity>,
    PagedListProvider.DataSourceFactory<UserListItem> {
    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, UserListItem> {
        return dao.getUserList(owner).map { it as UserListItem }
    }

    override suspend fun putList(
        entities: List<UserEntity>,
        query: ListQuery<QueryType.UserQueryType>?,
        owner: ListId?
    ) {
        dao.addUsers(entities.map { it.toEntity() }, owner)
    }

    override suspend fun clean(owner: ListId) {
        dao.clear(owner)
    }
}

class CustomTimelineListDao(
    private val dao: CustomTimelineDao
) : LocalListDataSource<QueryType.UserListMembership, CustomTimelineEntity>,
    PagedListProvider.DataSourceFactory<CustomTimelineItem> {
    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, CustomTimelineItem> {
        return dao.getCustomTimeline(owner).map { it as CustomTimelineItem }
    }

    override suspend fun putList(
        entities: List<CustomTimelineEntity>,
        query: ListQuery<QueryType.UserListMembership>?,
        owner: ListId?
    ) {
        dao.addCustomTimeline(entities, owner)
    }

    override suspend fun clean(owner: ListId) {
        dao.clean(owner)
    }
}
