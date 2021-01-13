package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.PagingSource
import androidx.room.withTransaction
import com.freshdigitable.udonroad2.data.LocalListDataSource
import com.freshdigitable.udonroad2.data.PagedListProvider
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineListDb
import com.freshdigitable.udonroad2.data.db.entity.ListDao
import com.freshdigitable.udonroad2.data.db.entity.TweetListEntity
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
import com.freshdigitable.udonroad2.model.TweetId
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

    override fun getDataSourceFactory(owner: ListId): PagingSource<Int, TweetListItem> {
        return dao.getTimeline(owner) as PagingSource<Int, TweetListItem>
    }

    override suspend fun putList(
        entities: PagedResponseList<TweetEntity>,
        query: ListQuery<QueryType.TweetQueryType>,
        owner: ListId
    ) {
        db.addTweetToListWithTransaction(entities, query, owner)
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = listDao.findListEntityById(id)

    override suspend fun clean(owner: ListId) {
        listDao.deleteList(owner)
    }

    companion object {
        private suspend fun AppDatabase.addTweetToListWithTransaction(
            entities: PagedResponseList<TweetEntity>,
            query: ListQuery<out QueryType.TweetQueryType>,
            owner: ListId
        ) = withTransaction {
            val listEntity = listDao().getListById(owner)
            tweetDao().addTweetsToList(entities, listEntity)
            listDao().updateCursorById(entities, query, owner)
        }
    }
}

class ConversationListDao(
    private val db: AppDatabase,
    private val tweetListDao: TweetListDao
) : LocalListDataSource<QueryType.TweetQueryType.Conversation, TweetEntity>,
    PagedListProvider.DataSourceFactory<TweetListItem> by tweetListDao {

    override suspend fun prepareList(
        query: QueryType.TweetQueryType.Conversation,
        owner: ListId
    ) {
        val tweetDao = db.tweetDao()
        val listDao = db.listDao()
        var id: TweetId? = query.tweetId
        val conversation = mutableListOf<TweetDao.ConversationEntity>()
        while (id != null) {
            val res = tweetDao.getConversationTweetIdsByTweetId(id, 20)
            conversation.addAll(res)
            if (res.size < 20) {
                break
            }
            id = res.last().replyTo
        }

        val entities = conversation.map { TweetListEntity(it.original, owner) }
        db.withTransaction {
            tweetDao.addTweetListEntities(entities)
            listDao.updateAppendCursorById(owner, conversation.last().replyTo?.value)
        }
    }

    override suspend fun findListEntity(id: ListId): ListEntity? = tweetListDao.findListEntity(id)

    override suspend fun putList(
        entities: PagedResponseList<TweetEntity>,
        query: ListQuery<QueryType.TweetQueryType.Conversation>,
        owner: ListId
    ) {
        tweetListDao.putList(entities, query as ListQuery<QueryType.TweetQueryType>, owner)
    }

    override suspend fun clean(owner: ListId) {
        tweetListDao.clean(owner)
    }
}

internal suspend fun TweetDao.addTweetsToList(
    tweet: PagedResponseList<TweetEntity>,
    listEntity: ListEntity
) {
    addTweets(tweet)
    addTweetListEntities(tweet.map { it.toListEntity(listEntity.id) })
    addReactions(tweet, listEntity.ownerId)
}

class UserListDao(
    private val db: AppDatabase,
) : LocalListDataSource<QueryType.UserQueryType, UserEntity>,
    PagedListProvider.DataSourceFactory<UserListItem> {
    private val dao: UserDao = db.userDao()
    private val listDao: ListDao = db.listDao()

    override fun getDataSourceFactory(owner: ListId): PagingSource<Int, UserListItem> {
        return dao.getUserList(owner) as PagingSource<Int, UserListItem>
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

    override fun getDataSourceFactory(owner: ListId): PagingSource<Int, CustomTimelineItem> {
        return dao.getCustomTimeline(owner) as PagingSource<Int, CustomTimelineItem>
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

    override suspend fun clean(owner: ListId) {
        listDao.deleteList(owner)
    }
}
