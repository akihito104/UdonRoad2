package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toUserListPagedList
import com.freshdigitable.udonroad2.data.restclient.ext.toUserPagedList
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.UserQueryType
import com.freshdigitable.udonroad2.model.user.User
import twitter4j.Twitter
import javax.inject.Inject

private class PagedListDataSource<Q : QueryType, E>(
    private val twitter: AppTwitter,
    private val fetchBlock: Twitter.(ListQuery<Q>, Long) -> PagedResponseList<E>
) : RemoteListDataSource<Q, E> {

    var nextCursor: Long = -1

    override suspend fun getList(query: ListQuery<Q>): List<E> {
        return when (nextCursor) {
            0L -> emptyList()
            else -> twitter.fetch {
                fetchBlock(query, nextCursor).also { nextCursor = it.nextCursor }
            }
        }
    }
}

class PagedResponseList<E>(
    val list: List<E>,
    val nextCursor: Long
) : List<E> by list

class FollowerListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<UserQueryType.Follower, User> by PagedListDataSource(
    twitter,
    { query, nextCursor -> getFollowersList(query.type.userId.value, nextCursor).toUserPagedList() }
)

class FollowingListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<UserQueryType.Following, User> by PagedListDataSource(
    twitter,
    { query, nextCursor -> getFriendsList(query.type.userId.value, nextCursor).toUserPagedList() }
)

class ListMembershipListDataSource @Inject constructor(
    twitter: AppTwitter
) : RemoteListDataSource<QueryType.UserListMembership, MemberList> by PagedListDataSource(twitter,
    { query, nextCursor ->
        getUserListMemberships(
            query.type.userId.value, query.pageOption.count, nextCursor
        ).toUserListPagedList()
    }
)
