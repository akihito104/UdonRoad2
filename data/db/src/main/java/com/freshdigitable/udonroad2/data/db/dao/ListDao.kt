package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserListItem

class TweetListDao(
    private val dao: TweetDao,
    private val listDao: ListDao,
) : LocalListDataSource<QueryType.TweetQueryType, TweetEntity>,
    PagedListProvider.DataSourceFactory<TweetListItem> {
    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, TweetListItem> {
        return dao.getTimeline(owner).map { it as TweetListItem }
    }

    // fixme: transaction
    override suspend fun putList(
        entities: PagedResponseList<TweetEntity>,
        query: ListQuery<QueryType.TweetQueryType>,
        owner: ListId
    ) {
        dao.addTweetsToList(entities, owner)
        if (query.pageOption is PageOption.OnInit || query.pageOption is PageOption.OnHead) {
            entities.prependCursor?.let {
                listDao.updatePrependCursorById(owner, it)
            }
        }
        if (query.pageOption is PageOption.OnInit || query.pageOption is PageOption.OnTail) {
            listDao.updateAppendCursorById(owner, entities.appendCursor)
        }
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)
    override suspend fun getListItemCount(id: ListId): Int = dao.getItemCountByListId(id)

    override suspend fun clean(owner: ListId) {
        dao.clear(owner)
    }
}

class UserListDao(
    private val dao: UserDao,
    private val listDao: ListDao,
) : LocalListDataSource<QueryType.UserQueryType, UserEntity>,
    PagedListProvider.DataSourceFactory<UserListItem> {
    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, UserListItem> {
        return dao.getUserList(owner).map { it as UserListItem }
    }

    override suspend fun putList(
        entities: PagedResponseList<UserEntity>,
        query: ListQuery<QueryType.UserQueryType>,
        owner: ListId
    ) {
        dao.addUsers(entities, owner)
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)
    override suspend fun getListItemCount(id: ListId): Int = dao.getItemCountByListId(id)

    override suspend fun clean(owner: ListId) {
        dao.clear(owner)
    }
}

class CustomTimelineListDao(
    private val dao: CustomTimelineDao,
    private val listDao: ListDao,
) : LocalListDataSource<QueryType.UserListMembership, CustomTimelineEntity>,
    PagedListProvider.DataSourceFactory<CustomTimelineItem> {
    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, CustomTimelineItem> {
        return dao.getCustomTimeline(owner).map { it as CustomTimelineItem }
    }

    override suspend fun putList(
        entities: PagedResponseList<CustomTimelineEntity>,
        query: ListQuery<QueryType.UserListMembership>,
        owner: ListId
    ) {
        dao.addCustomTimeline(entities, owner)
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)
    override suspend fun getListItemCount(id: ListId): Int = dao.getItemCountByListId(id)

    override suspend fun clean(owner: ListId) {
        dao.clear(owner)
    }
}
