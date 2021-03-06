package com.freshdigitable.udonroad2.data.db.dbview

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserListItem

@DatabaseView(
    viewName = "view_user_item",
    value =
    """
     SELECT id, name, screen_name, icon_url, description,
      follower_count, following_count, is_verified, is_protected
     FROM user
    """
)
internal data class UserListDbView(
    @ColumnInfo(name = "id")
    override val id: UserId,

    @ColumnInfo(name = "name")
    override val name: String,

    @ColumnInfo(name = "screen_name")
    override val screenName: String,

    @ColumnInfo(name = "icon_url")
    override val iconUrl: String,

    @ColumnInfo(name = "description")
    override val description: String,

    @ColumnInfo(name = "follower_count")
    override val followerCount: Int,

    @ColumnInfo(name = "following_count")
    override val followingCount: Int,

    @ColumnInfo(name = "is_verified")
    override val isVerified: Boolean,

    @ColumnInfo(name = "is_protected")
    override val isProtected: Boolean
) : UserListItem
