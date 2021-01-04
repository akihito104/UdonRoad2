package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import androidx.room.withTransaction
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineListDb
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import com.freshdigitable.udonroad2.data.db.entity.UserListEntity
import com.freshdigitable.udonroad2.data.db.entity.updateCursorById
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.data.db.ext.toListEntity
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.CustomTimelineItem
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserListItem

class TweetListDao(
    private val db: AppDatabase,
) : LocalListDataSource<QueryType.TweetQueryType, TweetEntity>,
    PagedListProvider.DataSourceFactory<TweetListItem> {
    private val dao: TweetDao = db.tweetDao()
    private val listDao: ListDao = db.listDao()

    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, TweetListItem> {
        return dao.getTimeline(owner).map { it as TweetListItem }
    }

    override suspend fun putList(
        entities: PagedResponseList<TweetEntity>,
        query: ListQuery<QueryType.TweetQueryType>,
        owner: ListId
    ) {
        db.withTransaction {
            addTweetsToList(entities, owner)
            listDao.updateCursorById(entities, query, owner)
        }
    }

    private suspend fun addTweetsToList(
        tweet: PagedResponseList<TweetEntity>,
        owner: ListId
    ) = with(dao) {
        addTweets(tweet)
        addTweetListEntities(tweet.map { it.toListEntity(owner) })

        val listOwner = db.listDao().getListById(owner)
        addReactions(tweet, listOwner.ownerId)
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)
    override suspend fun getListItemCount(id: ListId): Int = dao.getItemCountByListId(id)

    override suspend fun clean(owner: ListId) {
        listDao.deleteList(owner)
    }
}

class UserListDao(
    private val db: AppDatabase,
) : LocalListDataSource<QueryType.UserQueryType, UserEntity>,
    PagedListProvider.DataSourceFactory<UserListItem> {
    private val dao: UserDao = db.userDao()
    private val listDao: ListDao = db.listDao()

    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, UserListItem> {
        return dao.getUserList(owner).map { it as UserListItem }
    }

    override suspend fun putList(
        entities: PagedResponseList<UserEntity>,
        query: ListQuery<QueryType.UserQueryType>,
        owner: ListId
    ) {
        val listEntities = entities.map {
            UserListEntity(userId = it.id, listId = owner)
        }
        db.withTransaction {
            dao.addUsers(entities.map { it.toEntity() })
            dao.addUserListEntities(listEntities)

            listDao.updateCursorById(entities, query, owner)
        }
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)
    override suspend fun getListItemCount(id: ListId): Int = dao.getItemCountByListId(id)

    override suspend fun clean(owner: ListId) {
        listDao.deleteList(owner)
    }
}

class CustomTimelineListDao(
    private val db: AppDatabase,
) : LocalListDataSource<QueryType.UserListMembership, CustomTimelineEntity>,
    PagedListProvider.DataSourceFactory<CustomTimelineItem> {
    private val dao: CustomTimelineDao = db.customTimelineDao()
    private val listDao: ListDao = db.listDao()

    override fun getDataSourceFactory(owner: ListId): DataSource.Factory<Int, CustomTimelineItem> {
        return dao.getCustomTimeline(owner).map { it as CustomTimelineItem }
    }

    override suspend fun putList(
        entities: PagedResponseList<CustomTimelineEntity>,
        query: ListQuery<QueryType.UserListMembership>,
        owner: ListId
    ) {
        val users = entities.map { it.user.toEntity() }
        val customTimelines = entities.map { it.toEntity() }
        val listEntity = entities.map { e ->
            CustomTimelineListDb(customTimelineId = e.id, listId = owner)
        }
        db.withTransaction {
            db.userDao().addUsers(users)

            dao.addCustomTimelineEntities(customTimelines)
            dao.addCustomTimelineListEntities(listEntity)

            listDao.updateCursorById(entities, query, owner)
        }
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)
    override suspend fun getListItemCount(id: ListId): Int = dao.getItemCountByListId(id)

    override suspend fun clean(owner: ListId) {
        listDao.deleteList(owner)
    }
}
