package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.freshdigitable.udonroad2.model.MemberListId
import com.freshdigitable.udonroad2.model.user.UserId

@Entity(
    tableName = "custom_timeline",
    foreignKeys = [
        ForeignKey(entity = UserEntityDb::class, parentColumns = ["id"], childColumns = ["user_id"])
    ]
)
internal data class CustomTimelineDb(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: MemberListId,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "user_id", index = true)
    val userId: UserId,

    @ColumnInfo(name = "member_count")
    val memberCount: Int,

    @ColumnInfo(name = "follower_count")
    val followerCount: Int,

    @ColumnInfo(name = "is_public")
    val isPublic: Boolean
)
