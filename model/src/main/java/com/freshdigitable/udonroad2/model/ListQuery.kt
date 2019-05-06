package com.freshdigitable.udonroad2.model

sealed class ListQuery(
    open val userId: Long?
) {
    data class Timeline(
        override val userId: Long? = null
    ) : ListQuery(userId)

    data class Fav(
        override val userId: Long? = null
    ) : ListQuery(userId)
}
