package com.freshdigitable.udonroad2.model

import java.io.Serializable

data class ListQuery<T : QueryType>(
    val type: T,
    val pageOption: PageOption = PageOption.OnInit
)

sealed class QueryType(
    open val userId: Long?
) : Serializable {

    sealed class TweetQueryType(
        userId: Long?
    ) : QueryType(userId) {
        data class Timeline(
            override val userId: Long? = null
        ) : TweetQueryType(userId)

        data class Fav(
            override val userId: Long? = null
        ) : TweetQueryType(userId)

        data class Media(
            private val screenName: String
        ) : TweetQueryType(null) {
            val query: String
                get() = "from:$screenName filter:media exclude:retweets"
        }
    }

    sealed class UserQueryType(
        userId: Long?
    ) : QueryType(userId) {
        data class Follower(
            override val userId: Long
        ) : UserQueryType(userId)

        data class Following(
            override val userId: Long
        ) : UserQueryType(userId)
    }

    data class UserListMembership(
        override val userId: Long
    ) : QueryType(userId)

    object Oauth : QueryType(null)
}

private const val FETCH_COUNT = 50

sealed class PageOption(
    open val page: Int = -1,
    open val count: Int = FETCH_COUNT,
    open val sinceId: Long = -1,
    open val maxId: Long = -1
) {
    object OnInit : PageOption()

    data class OnHead(
        override val sinceId: Long = -1,
        override val count: Int = FETCH_COUNT
    ) : PageOption(page = 1, count = count, sinceId = sinceId)

    data class OnTail(
        override val maxId: Long = -1,
        override val count: Int = FETCH_COUNT
    ) : PageOption(page = 1, count = count, sinceId = 1, maxId = maxId)
}
