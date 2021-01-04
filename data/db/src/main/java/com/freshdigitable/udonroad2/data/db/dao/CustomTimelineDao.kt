package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freshdigitable.udonroad2.data.db.dbview.CustomTimelineListItemDb
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineDb
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineListDb
import com.freshdigitable.udonroad2.model.ListId

@Dao
abstract class CustomTimelineDao {
    @Query(
        """
        SELECT v.* FROM custom_timeline_list AS l
        INNER JOIN view_custom_timeline_item AS v ON l.custom_timeline_id = v.id
        WHERE l.list_id = :owner
        ORDER BY l.`order` ASC"""
    )
    internal abstract fun getCustomTimeline(
        owner: ListId
    ): DataSource.Factory<Int, CustomTimelineListItemDb>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addCustomTimelineEntities(entities: List<CustomTimelineDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addCustomTimelineListEntities(
        entities: List<CustomTimelineListDb>
    )

    @Query("DELETE FROM custom_timeline_list WHERE list_id = :owner")
    internal abstract suspend fun deleteByListId(owner: ListId)

    @Query("SELECT COUNT() FROM custom_timeline_list WHERE list_id = :id")
    internal abstract suspend fun getItemCountByListId(id: ListId): Int
}
