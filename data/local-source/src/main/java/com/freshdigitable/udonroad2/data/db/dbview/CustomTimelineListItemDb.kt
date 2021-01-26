package com.freshdigitable.udonroad2.data.db.dbview

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import com.freshdigitable.udonroad2.model.CustomTimelineId
import com.freshdigitable.udonroad2.model.CustomTimelineItem

private const val PREFIX_OWNER = "user_"

@DatabaseView(
    """
    SELECT m.id, m.name, m.description, m.member_count, m.follower_count, m.is_public,
     u.id AS user_id,
     u.name AS ${PREFIX_OWNER}name,
     u.screen_name AS ${PREFIX_OWNER}screen_name,
     u.icon_url AS ${PREFIX_OWNER}icon_url,
     u.is_protected AS ${PREFIX_OWNER}is_protected,
     u.is_verified AS ${PREFIX_OWNER}is_verified
    FROM custom_timeline AS m
    INNER JOIN view_user_item AS u ON m.user_id = u.id
""",
    viewName = "view_custom_timeline_item"
)
internal data class CustomTimelineListItemDb(
    @ColumnInfo(name = "id")
    override val id: CustomTimelineId,

    @ColumnInfo(name = "name")
    override val name: String,

    @ColumnInfo(name = "description")
    override val description: String,

    @Embedded(prefix = PREFIX_OWNER)
    override val user: TweetUserItemDb,

    @ColumnInfo(name = "member_count")
    override val memberCount: Int,

    @ColumnInfo(name = "follower_count")
    override val followerCount: Int,

    @ColumnInfo(name = "is_public")
    override val isPublic: Boolean
) : CustomTimelineItem
