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
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.model.CustomTimelineEntity

@Dao
abstract class CustomTimelineDao(
    private val db: AppDatabase
) {
    @Query(
        """
        SELECT v.* FROM custom_timeline_list AS l
        INNER JOIN view_custom_timeline_item AS v ON l.custom_timeline_id = v.id
        WHERE l.owner = :owner
        ORDER BY l.`order` ASC"""
    )
    internal abstract fun getCustomTimeline(owner: String): DataSource.Factory<Int, CustomTimelineListItemDb>

    @Transaction
    internal open suspend fun addCustomTimeline(
        entities: List<CustomTimelineEntity>,
        owner: String?
    ) {
        val users = entities.map { it.user.toEntity() }
        val customTimelines = entities.map { it.toEntity() }
        db.userDao().addUsers(users)
        addCustomTimelineEntities(customTimelines)

        if (owner != null) {
            val listEntity = entities.map { e ->
                CustomTimelineListDb(customTimelineId = e.id, owner = owner)
            }
            addCustomTimelineListEntities(listEntity)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addCustomTimelineEntities(entities: List<CustomTimelineDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addCustomTimelineListEntities(entities: List<CustomTimelineListDb>)

    @Query("DELETE FROM custom_timeline_list WHERE owner = :owner")
    abstract suspend fun clean(owner: String)
}
