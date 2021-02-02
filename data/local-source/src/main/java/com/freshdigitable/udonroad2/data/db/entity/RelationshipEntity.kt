package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.Relationship

@Entity(
    tableName = "relationship",
    primaryKeys = ["user_id", "source_user_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
        ),
        ForeignKey(
            entity = UserEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["source_user_id"],
        )
    ]
)
internal data class RelationshipEntity(
    @ColumnInfo(name = "user_id", index = true)
    override val targetUserId: UserId,

    @ColumnInfo(name = "following")
    override val following: Boolean,

    @ColumnInfo(name = "blocking")
    override val blocking: Boolean,

    @ColumnInfo(name = "muting")
    override val muting: Boolean,

    @ColumnInfo(name = "want_retweets")
    override val wantRetweets: Boolean,

    @ColumnInfo(name = "notifications_enabled")
    override val notificationsEnabled: Boolean,

    @ColumnInfo(name = "source_user_id", index = true)
    override val sourceUserId: UserId,
) : Relationship
