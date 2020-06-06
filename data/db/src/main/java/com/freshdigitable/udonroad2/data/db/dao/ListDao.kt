package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.MemberListItem
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.model.UserListItem

class TweetListDao(
    private val dao: TweetDao
) : LocalListDataSource<ListQuery.TweetListQuery, TweetEntity>,
    PagedListProvider.DataSourceFactory<TweetListItem> {
    override fun getDataSourceFactory(owner: String): DataSource.Factory<Int, TweetListItem> {
        return dao.getTimeline(owner).map { it as TweetListItem }
    }

    override suspend fun putList(
        entities: List<TweetEntity>,
        query: ListQuery.TweetListQuery?,
        owner: String?
    ) {
        dao.addTweets(entities, owner)
    }

    override suspend fun clean(owner: String) {
        dao.clear(owner)
    }
}

class UserListDao(
    private val dao: UserDao
) : LocalListDataSource<ListQuery.UserListQuery, User>,
    PagedListProvider.DataSourceFactory<UserListItem> {
    override fun getDataSourceFactory(owner: String): DataSource.Factory<Int, UserListItem> {
        return dao.getUserList(owner).map { it as UserListItem }
    }

    override suspend fun putList(
        entities: List<User>,
        query: ListQuery.UserListQuery?,
        owner: String?
    ) {
        dao.addUsers(entities.map { it.toEntity() }, owner)
    }

    override suspend fun clean(owner: String) {
        dao.clear(owner)
    }
}

class MemberListListDao(
    private val dao: MemberListDao
) : LocalListDataSource<ListQuery.UserListMembership, MemberList>,
    PagedListProvider.DataSourceFactory<MemberListItem> {
    override fun getDataSourceFactory(owner: String): DataSource.Factory<Int, MemberListItem> {
        return dao.getMemberList(owner).map { it as MemberListItem }
    }

    override suspend fun putList(
        entities: List<MemberList>,
        query: ListQuery.UserListMembership?,
        owner: String?
    ) {
        dao.addMemberList(entities, owner)
    }

    override suspend fun clean(owner: String) {
        dao.clean(owner)
    }
}
