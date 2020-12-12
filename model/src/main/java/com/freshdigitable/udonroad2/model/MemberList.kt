package com.freshdigitable.udonroad2.model

import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import java.io.Serializable

interface MemberList : MemberListItem {
    override val user: UserEntity
}

interface MemberListItem {
    val id: MemberListId
    val name: String
    val description: String
    val user: TweetUserItem
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
