package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.data.restclient.ext.toPagedResponseList
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.UserQueryType
import com.freshdigitable.udonroad2.model.user.UserEntity
import twitter4j.Twitter
import twitter4j.User
import twitter4j.UserList
import javax.inject.Inject

private class PagedListDataSource<Q : QueryType, E : Any>(
    private val twitter: AppTwitter,
    private val fetchBlock: Twitter.(ListQuery<Q>, Long) -> PagedResponseList<E>
) : RemoteListDataSource<Q, E> {

    @Deprecated("to be deleted")
    var nextCursor: Long = -1

    override suspend fun getList(query: ListQuery<Q>): PagedResponseList<E> {
        return when (nextCursor) {
            0L -> PagedResponseList(emptyList())
            else -> twitter.fetch {
                fetchBlock(query, nextCursor).also { nextCursor = it.nextCursor ?: 0 }
            }
        }
    }
}

class FollowerListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<UserQueryType.Follower, UserEntity> by PagedListDataSource(
    twitter,
    { query, nextCursor ->
        getFollowersList(query.type.userId.value, nextCursor)
            .toPagedResponseList(User::toEntity)
    }
)

class FollowingListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<UserQueryType.Following, UserEntity> by PagedListDataSource(
    twitter,
    { query, nextCursor ->
        getFriendsList(query.type.userId.value, nextCursor).toPagedResponseList(User::toEntity)
    }
)

class ListMembershipListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<QueryType.UserListMembership, CustomTimelineEntity> by PagedListDataSource(
    twitter,
    { query, nextCursor ->
        getUserListMemberships(
            query.type.userId.value, query.pageOption.count, nextCursor
        ).toPagedResponseList(UserList::toEntity)
    }
)
