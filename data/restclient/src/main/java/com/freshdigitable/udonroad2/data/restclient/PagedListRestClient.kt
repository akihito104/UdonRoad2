package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toUserListPagedList
import com.freshdigitable.udonroad2.data.restclient.ext.toUserPagedList
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.MemberList
import com.freshdigitable.udonroad2.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Paging
import twitter4j.Twitter
import javax.inject.Inject

abstract class PagedListRestClient<Q : ListQuery, E>(
    private val twitter: Twitter
) : ListRestClient<Q, E> {

    override lateinit var query: Q

    protected var nextCursor: Long = -1

    protected abstract val fetchBlock: suspend Twitter.(Q) -> PagedResponseList<E>

    override suspend fun fetchTimeline(paging: Paging?): List<E> = withContext(Dispatchers.IO) {
        if (nextCursor == 0L) {
            return@withContext listOf<E>()
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

class FollowerListClient @Inject constructor(
    twitter: Twitter
) : PagedListRestClient<ListQuery.Follower, User>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery.Follower) -> PagedResponseList<User>
        get() = { query -> getFollowersList(query.userId, nextCursor).toUserPagedList() }
}

class FollowingListClient @Inject constructor(
    twitter: Twitter
) : PagedListRestClient<ListQuery.Following, User>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery.Following) -> PagedResponseList<User>
        get() = { query -> getFriendsList(query.userId, nextCursor).toUserPagedList() }
}

class ListMembershipListClient @Inject constructor(
    twitter: Twitter
) : PagedListRestClient<ListQuery.UserListMembership, MemberList>(twitter) {

    override val fetchBlock: suspend Twitter.(ListQuery.UserListMembership) -> PagedResponseList<MemberList>
        get() = { query ->
            getUserListMemberships(
                query.userId,
                50,
                nextCursor
            ).toUserListPagedList()
        }
}
