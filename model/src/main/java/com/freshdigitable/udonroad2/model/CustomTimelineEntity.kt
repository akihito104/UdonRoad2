package com.freshdigitable.udonroad2.model

import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity

interface CustomTimelineEntity : CustomTimelineItem {
    override val user: UserEntity
}

interface CustomTimelineItem {
    val id: CustomTimelineId
    val name: String
    val description: String
    val user: TweetUserItem
    val memberCount: Int
    val followerCount: Int
    val isPublic: Boolean

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
