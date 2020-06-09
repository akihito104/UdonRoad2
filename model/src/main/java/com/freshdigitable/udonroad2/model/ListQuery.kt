package com.freshdigitable.udonroad2.model

import java.io.Serializable

sealed class ListQuery(
    open val userId: Long?,
    open val pageOption: PageOption = PageOption.OnInit
) : Serializable {

    sealed class TweetListQuery(
        userId: Long?,
        pageOption: PageOption = PageOption.OnInit
    ) : ListQuery(userId, pageOption) {
        data class Timeline(
            override val userId: Long? = null,
            override val pageOption: PageOption = PageOption.OnInit
        ) : TweetListQuery(userId, pageOption)

        data class Fav(
            override val userId: Long? = null,
            override val pageOption: PageOption = PageOption.OnInit
        ) : TweetListQuery(userId, pageOption)

        data class Media(
            private val screenName: String,
            override val pageOption: PageOption = PageOption.OnInit
        ) : TweetListQuery(null, pageOption) {
            val query: String
                get() = "from:$screenName filter:media exclude:retweets"
        }
    }

    sealed class UserListQuery(
        userId: Long?,
        pageOption: PageOption = PageOption.OnInit
    ) : ListQuery(userId, pageOption) {
        data class Follower(
            override val userId: Long,
            override val pageOption: PageOption = PageOption.OnInit
        ) : UserListQuery(userId)

        data class Following(
            override val userId: Long,
            override val pageOption: PageOption = PageOption.OnInit
        ) : UserListQuery(userId)
    }

    data class UserListMembership(
        override val userId: Long
    ) : ListQuery(userId)

    object Oauth : ListQuery(null)
}

sealed class PageOption(
    open val page: Int = -1,
    open val count: Int = -1,
    open val sinceId: Long = -1,
    open val maxId: Long = -1
) {
    object OnInit : PageOption()

    data class OnHead(
        override val count: Int,
        override val sinceId: Long
    ) : PageOption(page = 1, count = count, sinceId = sinceId)

    data class OnTail(
        override val count: Int,
        override val maxId: Long
    ) : PageOption(page = 1, count = count, sinceId = 1, maxId = maxId)
}
