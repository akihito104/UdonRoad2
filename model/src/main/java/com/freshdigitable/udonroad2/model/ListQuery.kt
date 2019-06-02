package com.freshdigitable.udonroad2.model

import java.io.Serializable

sealed class ListQuery(
    open val userId: Long?
) : Serializable {

    data class Timeline(
        override val userId: Long? = null
    ) : ListQuery(userId)

    data class Fav(
        override val userId: Long? = null
    ) : ListQuery(userId)

    data class Media(
        private val screenName: String
    ) : ListQuery(null) {
        val query: String
            get() = "from:$screenName filter:media exclude:retweets"
    }
}
