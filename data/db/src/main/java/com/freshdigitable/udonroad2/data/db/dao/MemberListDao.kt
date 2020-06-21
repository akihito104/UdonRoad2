package com.freshdigitable.udonroad2.data.db.dao

import androidx.paging.DataSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dbview.MemberListDbView
import com.freshdigitable.udonroad2.data.db.entity.MemberListEntity
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.model.MemberList

@Dao
abstract class MemberListDao(
    private val db: AppDatabase
) {
    @Query(
        """
        SELECT v.* FROM member_list_list AS l
        INNER JOIN view_member_list AS v ON l.member_list_id = v.id
        WHERE l.owner = :owner
        ORDER BY l.`order` ASC"""
    )
    internal abstract fun getMemberList(owner: String): DataSource.Factory<Int, MemberListDbView>

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
                MemberListListEntity(memberListId = e.id, owner = owner)
            }
            addMemberListListEntities(listEntity)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addMemberListEntities(entities: List<MemberListEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addMemberListListEntities(entities: List<MemberListListEntity>)

    @Query("DELETE FROM member_list_list WHERE owner = :owner")
    abstract suspend fun clean(owner: String)
}

@Entity(
    tableName = "member_list_list",
    foreignKeys = [
        ForeignKey(
            entity = MemberListEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_list_id"]
        )
    ]
)
internal data class MemberListListEntity(
    @ColumnInfo(name = "member_list_id", index = true)
    val memberListId: Long,

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "order")
    val order: Long = 0,

    @ColumnInfo(name = "owner")
    val owner: String
)
