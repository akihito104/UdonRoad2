package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "member_list",
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"])
    ]
)
internal data class MemberListEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "user_id", index = true)
    val userId: Long,

    @ColumnInfo(name = "member_count")
    val memberCount: Int,

    @ColumnInfo(name = "follower_count")
    val followerCount: Int,

    @ColumnInfo(name = "is_public")
    val isPublic: Boolean
)
