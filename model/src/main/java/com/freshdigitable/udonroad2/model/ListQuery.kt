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
}
