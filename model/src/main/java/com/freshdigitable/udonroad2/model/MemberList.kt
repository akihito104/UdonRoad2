package com.freshdigitable.udonroad2.model

interface MemberList : MemberListItem {
    override val user: User
}

interface MemberListItem {
    val id: Long
    val name: String
    val description: String
    val user: TweetingUser
    val memberCount: Int
    val followerCount: Int
    val isPublic: Boolean

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
