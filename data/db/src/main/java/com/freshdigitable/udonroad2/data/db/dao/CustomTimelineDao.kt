package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dbview.CustomTimelineListItemDb
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineDb
import com.freshdigitable.udonroad2.data.db.entity.CustomTimelineListDb
import com.freshdigitable.udonroad2.data.db.entity.updateCursorById
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.model.CustomTimelineEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.PagedResponseList

@Dao
abstract class CustomTimelineDao(
    private val db: AppDatabase
) {
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

    @Transaction
    internal open suspend fun addCustomTimeline(
        entities: PagedResponseList<CustomTimelineEntity>,
        owner: ListId?
    ) {
        val users = entities.map { it.user.toEntity() }
        val customTimelines = entities.map { it.toEntity() }
        db.userDao().addUsers(users)
        addCustomTimelineEntities(customTimelines)

        if (owner != null) {
            db.listDao().updateCursorById(entities, owner)
            val listEntity = entities.map { e ->
                CustomTimelineListDb(customTimelineId = e.id, listId = owner)
            }
            addCustomTimelineListEntities(listEntity)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addCustomTimelineEntities(entities: List<CustomTimelineDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addCustomTimelineListEntities(
        entities: List<CustomTimelineListDb>
    )

    @Query("DELETE FROM custom_timeline_list WHERE list_id = :owner")
    internal abstract suspend fun deleteByListId(owner: ListId)

    @Query("SELECT COUNT() FROM custom_timeline_list WHERE list_id = :id")
    abstract suspend fun getItemCountByListId(id: ListId): Int

    suspend fun clear(owner: ListId) {
        db.listDao().deleteList(owner)
    }
}
