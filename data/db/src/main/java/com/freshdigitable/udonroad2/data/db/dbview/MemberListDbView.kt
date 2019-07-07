package com.freshdigitable.udonroad2.data.db.dbview

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import com.freshdigitable.udonroad2.model.MemberListItem

@DatabaseView("""
    SELECT m.id, m.name, m.description, m.member_count, m.follower_count, m.is_public,
     u.id AS user_id,
     u.name AS user_name,
     u.screen_name AS user_screen_name,
     u.icon_url AS user_icon_url
    FROM MemberListEntity AS m
    INNER JOIN view_user_in_tweet AS u ON m.user_id = u.id
""", viewName = "view_member_list")
internal data class MemberListDbView(
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "name")
    override val name: String,

    @ColumnInfo(name = "description")
    override val description: String,

    @Embedded(prefix = "user_")
    override val user: TweetingUser,

    @ColumnInfo(name = "member_count")
    override val memberCount: Int,

    @ColumnInfo(name = "follower_count")
    override val followerCount: Int,

    @ColumnInfo(name = "is_public")
    override val isPublic: Boolean
) : MemberListItem
