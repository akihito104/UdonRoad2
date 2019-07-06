package com.freshdigitable.udonroad2.model

import org.threeten.bp.Instant


interface TweetListItem {

    val originalId: Long

    val originalUser: TweetingUser

    val body: Tweet

    val quoted: Tweet?

    val isRetweet: Boolean
        get() = originalId != body.id

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

interface Tweet {

    val id: Long

    val text: String

    val retweetCount: Int

    val favoriteCount: Int

    val user: TweetingUser

    val source: String

    val createdAt: Instant
}
