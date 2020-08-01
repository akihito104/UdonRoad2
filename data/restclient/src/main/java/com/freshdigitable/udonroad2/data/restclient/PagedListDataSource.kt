package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toUserListPagedList
import com.freshdigitable.udonroad2.data.restclient.ext.toUserPagedList
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.UserQueryType
import com.freshdigitable.udonroad2.model.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import javax.inject.Inject

abstract class PagedListDataSource<Q : QueryType, E>(
    private val twitter: Twitter
) : RemoteListDataSource<Q, E> {

    protected var nextCursor: Long = -1

    protected abstract val fetchBlock: suspend Twitter.(ListQuery<Q>) -> PagedResponseList<E>

    override suspend fun getList(query: ListQuery<Q>): List<E> = withContext(Dispatchers.IO) {
        when (nextCursor) {
            0L -> emptyList<E>()
            else -> fetchBlock(twitter, query).also { nextCursor = it.nextCursor }
        }
    }
}

class PagedResponseList<E>(
    val list: List<E>,
    val nextCursor: Long
) : List<E> by list

class FollowerListDataSource @Inject constructor(
    twitter: Twitter
) : PagedListDataSource<UserQueryType.Follower, User>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery<UserQueryType.Follower>) -> PagedResponseList<User>
        get() = { query -> getFollowersList(query.type.userId.value, nextCursor).toUserPagedList() }
}

class FollowingListDataSource @Inject constructor(
    twitter: Twitter
) : PagedListDataSource<UserQueryType.Following, User>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery<UserQueryType.Following>) -> PagedResponseList<User>
        get() = { query -> getFriendsList(query.type.userId.value, nextCursor).toUserPagedList() }
}

class ListMembershipListDataSource @Inject constructor(
    twitter: Twitter
) : PagedListDataSource<QueryType.UserListMembership, MemberList>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery<QueryType.UserListMembership>) -> PagedResponseList<MemberList>
        get() = { query ->
            getUserListMemberships(
                query.type.userId.value,
                query.pageOption.count,
                nextCursor
            ).toUserListPagedList()
        }
}
