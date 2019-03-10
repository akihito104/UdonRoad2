package com.freshdigitable.udonroad2.model

import org.threeten.bp.Instant


interface TweetListItem {

    val originalId: Long

    val originalUser: User

    val body: Tweet

    val quoted: Tweet?

    val isRetweet: Boolean
        get() = originalId != body.id
}

interface Tweet {

    val id: Long

    val text: String

    val retweetCount: Int

    val favoriteCount: Int

    val user: User

    val source: String

    val createdAt: Instant
}
