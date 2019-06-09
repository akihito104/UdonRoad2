package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import com.freshdigitable.udonroad2.data.db.entity.UserEntity
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetListItem
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.model.UserListItem

interface ListDao<E, I> {
    fun getList(owner: String): DataSource.Factory<Int, I>
    suspend fun addEntities(entities: List<E>, owner: String? = null)
    suspend fun clean(owner: String)
}

class TweetListDao(
    private val dao: TweetDao
) : ListDao<TweetEntity, TweetListItem> {
    override fun getList(owner: String): DataSource.Factory<Int, TweetListItem> {
        return dao.getTimeline(owner).map { it as TweetListItem }
    }

    override suspend fun addEntities(entities: List<TweetEntity>, owner: String?) {
        dao.addTweets(entities, owner)
    }

    override suspend fun clean(owner: String) {
        dao.clear(owner)
    }
}

class UserListDao(
    private val dao: UserDao
) : ListDao<User, UserListItem> {
    override fun getList(owner: String): DataSource.Factory<Int, UserListItem> {
        return dao.getUserList(owner).map { it as UserListItem }
    }

    override suspend fun addEntities(entities: List<User>, owner: String?) {
        dao.addUsers(entities.map { it.toEntity() }, owner)
    }

    override suspend fun clean(owner: String) {
        dao.clear(owner)
    }

    private fun User.toEntity(): UserEntity {
        return if (this is UserEntity) {
            return this
        } else {
            UserEntity(
                id,
                name,
                screenName,
                iconUrl,
                description,
                profileBannerImageUrl,
                followerCount,
                followingCount,
                tweetCount,
                favoriteCount,
                listedCount,
                profileLinkColor,
                location,
                url,
                verified,
                isProtected
            )
        }
    }
}
