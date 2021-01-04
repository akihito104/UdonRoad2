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
import javax.inject.Singleton

private class PagedListDataSource<Q : QueryType, E : Any>(
    private val twitter: AppTwitter,
    private val fetchBlock: Twitter.(ListQuery<Q>) -> PagedResponseList<E>
) : RemoteListDataSource<Q, E> {

    override suspend fun getList(query: ListQuery<Q>): PagedResponseList<E> = twitter.fetch {
        fetchBlock(query)
    }
}

@Singleton
class FollowerListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<UserQueryType.Follower, UserEntity> by PagedListDataSource(
    twitter,
    { query ->
        getFollowersList(query.type.userId.value, query.pageOption.maxId ?: -1)
            .toPagedResponseList(User::toEntity)
    }
)

@Singleton
class FollowingListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<UserQueryType.Following, UserEntity> by PagedListDataSource(
    twitter,
    { query ->
        getFriendsList(query.type.userId.value, query.pageOption.maxId ?: -1)
            .toPagedResponseList(User::toEntity)
    }
)

@Singleton
class ListMembershipListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<QueryType.UserListMembership, CustomTimelineEntity> by PagedListDataSource(
    twitter,
    { query ->
        getUserListMemberships(
            query.type.userId.value, query.pageOption.count, query.pageOption.maxId ?: -1
        ).toPagedResponseList(UserList::toEntity)
    }
)
