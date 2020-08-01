package com.freshdigitable.udonroad2.model

import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import java.io.Serializable

interface MemberList : MemberListItem {
    override val user: User
}

interface MemberListItem {
    val id: MemberListId
    val name: String
    val description: String
    val user: TweetingUser
    val memberCount: Int
    val followerCount: Int
    val isPublic: Boolean

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data class MemberListId(val value: Long) : Serializable {
    companion object {
        fun create(value: Long?): MemberListId? = value?.let { MemberListId(it) }
    }
}
