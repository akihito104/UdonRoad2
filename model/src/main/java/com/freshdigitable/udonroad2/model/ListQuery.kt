package com.freshdigitable.udonroad2.model

import java.io.Serializable

data class ListQuery<T : QueryType>(
    val type: T,
    val pageOption: PageOption = PageOption.OnInit,
)

sealed class QueryType(
    open val userId: UserId?,
) : Serializable {

    sealed class Tweet(
        userId: UserId?,
    ) : QueryType(userId) {
        data class Timeline(override val userId: UserId? = null) : Tweet(userId)
        data class Fav(override val userId: UserId? = null) : Tweet(userId)

        data class Media(private val screenName: String) : Tweet(null) {
            val query: String
                get() = "from:$screenName filter:media exclude:retweets"
        }

        data class Conversation(val tweetId: TweetId) : Tweet(null)

        data class CustomTimeline(
            val id: CustomTimelineId,
            val title: String,
        ) : Tweet(null)
    }

    sealed class User(
        userId: UserId?,
    ) : QueryType(userId) {
        data class Follower(override val userId: UserId) : User(userId)
        data class Following(override val userId: UserId) : User(userId)
    }

    sealed class CustomTimelineList(
        userId: UserId?,
    ) : QueryType(userId) {
        data class Membership(override val userId: UserId) : CustomTimelineList(userId)
        data class Ownership(override val userId: UserId? = null) : CustomTimelineList(userId)
    }

    object Oauth : QueryType(null)
}

private const val FETCH_COUNT = 50

sealed class PageOption(
    open val page: Int = 1,
    open val count: Int = FETCH_COUNT,
    open val sinceId: Long? = null,
    open val maxId: Long? = null,
) {
    object OnInit : PageOption()

    data class OnHead(
        val cursor: Long? = null,
        override val count: Int = FETCH_COUNT,
    ) : PageOption(page = 1, count = count, sinceId = cursor)

    data class OnTail(
        val cursor: Long? = null,
        override val count: Int = FETCH_COUNT,
    ) : PageOption(page = 1, count = count, maxId = cursor)
}
