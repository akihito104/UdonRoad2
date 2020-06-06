package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toUserListPagedList
import com.freshdigitable.udonroad2.data.restclient.ext.toUserPagedList
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.ListQuery.UserListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import javax.inject.Inject

abstract class PagedListDataSource<Q : ListQuery, E>(
    private val twitter: Twitter
) : RemoteListDataSource<Q, E> {

    protected var nextCursor: Long = -1

    protected abstract val fetchBlock: suspend Twitter.(Q) -> PagedResponseList<E>

    override suspend fun getList(query: Q): List<E> = withContext(Dispatchers.IO) {
        if (nextCursor == 0L) {
            return@withContext emptyList<E>()
        }
        val list = fetchBlock(twitter, query)
        nextCursor = list.nextCursor
        return@withContext list
    }
}

class PagedResponseList<E>(
    val list: List<E>,
    val nextCursor: Long
) : List<E> by list

class FollowerListDataSource @Inject constructor(
    twitter: Twitter
) : PagedListDataSource<UserListQuery.Follower, User>(twitter) {

    override val fetchBlock: suspend Twitter.(UserListQuery.Follower) -> PagedResponseList<User>
        get() = { query -> getFollowersList(query.userId, nextCursor).toUserPagedList() }
}

class FollowingListDataSource @Inject constructor(
    twitter: Twitter
) : PagedListDataSource<UserListQuery.Following, User>(twitter) {

    override val fetchBlock: suspend Twitter.(UserListQuery.Following) -> PagedResponseList<User>
        get() = { query -> getFriendsList(query.userId, nextCursor).toUserPagedList() }
}

class ListMembershipListDataSource @Inject constructor(
    twitter: Twitter
) : PagedListDataSource<ListQuery.UserListMembership, MemberList>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery.UserListMembership) -> PagedResponseList<MemberList>
        get() = { query ->
            getUserListMemberships(
                query.userId,
                50,
                nextCursor
            ).toUserListPagedList()
        }
}
