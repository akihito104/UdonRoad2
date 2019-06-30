package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.freshdigitable.udonroad2.model.Relationship

@Entity(
    tableName = "relationship",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"]
        )
    ]
)
data class RelationshipEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id", index = true)
    override val userId: Long,

    @ColumnInfo(name = "following")
    override val following: Boolean,

    @ColumnInfo(name = "blocking")
    override val blocking: Boolean,

    @ColumnInfo(name = "muting")
    override val muting: Boolean,

    @ColumnInfo(name = "want_retweets")
    override val wantRetweets: Boolean,

    @ColumnInfo(name = "notifications_enabled")
    override val notificationsEnabled: Boolean
) : Relationship
