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
import com.freshdigitable.udonroad2.model.MemberList

@Dao
abstract class MemberListDao(
    private val db: AppDatabase
) {
    @Query(
        """
        SELECT v.* FROM custom_timeline_list AS l
        INNER JOIN view_custom_timeline_item AS v ON l.member_list_id = v.id
        WHERE l.owner = :owner
        ORDER BY l.`order` ASC"""
    )
    internal abstract fun getMemberList(owner: String): DataSource.Factory<Int, CustomTimelineListItemDb>

    @Transaction
    internal open suspend fun addMemberList(
        entities: List<MemberList>,
        owner: String?
    ) {
        val users = entities.map { it.user.toEntity() }
        val memberLists = entities.map { it.toEntity() }
        db.userDao().addUsers(users)
        addMemberListEntities(memberLists)

        if (owner != null) {
            val listEntity = entities.map { e ->
                CustomTimelineListDb(memberListId = e.id, owner = owner)
            }
            addMemberListListEntities(listEntity)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addMemberListEntities(entities: List<CustomTimelineDb>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addMemberListListEntities(entities: List<CustomTimelineListDb>)

    @Query("DELETE FROM custom_timeline_list WHERE owner = :owner")
    abstract suspend fun clean(owner: String)
}
