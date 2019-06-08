package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetingUser
import twitter4j.Paging
import twitter4j.Twitter
import twitter4j.User
import javax.inject.Inject

class FollowerListClient @Inject constructor(
    val twitter: Twitter
) : ListRestClient<ListQuery.Follower, TweetingUser> {

    override lateinit var query: ListQuery.Follower
    private var nextCursor: Long? = null

    override suspend fun fetchTimeline(paging: Paging?): List<TweetingUser> {
        if (nextCursor != null && nextCursor == 0L) {
            return listOf()
        }
        val list = twitter.getFollowersList(query.userId, nextCursor ?: 0)
        nextCursor = if (list.hasNext()) {
            list.nextCursor
        } else {
            0
        }
        return list.map(User::toEntity)
    }
}

class FollowingListClient @Inject constructor(
    val twitter: Twitter
) : ListRestClient<ListQuery.Following, TweetingUser> {

    override lateinit var query: ListQuery.Following
    private var nextCursor: Long? = null

    override suspend fun fetchTimeline(paging: Paging?): List<TweetingUser> {
        if (nextCursor != null && nextCursor == 0L) {
            return listOf()
        }
        val list = twitter.getFriendsList(query.userId, nextCursor ?: 0)
        nextCursor = if (list.hasNext()) {
            list.nextCursor
        } else {
            0
        }
        return list.map(User::toEntity)
    }
}
