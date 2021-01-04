/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import com.freshdigitable.udonroad2.data.db.AppTypeConverter
import com.freshdigitable.udonroad2.model.ListEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.UserId

@Entity(tableName = "list")
data class ListEntityDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val _id: Int = 0,
    @ColumnInfo(name = "owner_id", index = true)
    val ownerId: UserId,
    @ColumnInfo(name = "prepend_cursor")
    override val prependCursor: Long = ListEntity.CURSOR_INIT,
    @ColumnInfo(name = "append_cursor")
    override val appendCursor: Long? = ListEntity.CURSOR_INIT,
) : ListEntity {
    @Ignore
    override val id: ListId = ListId(_id)
}

class ListIdConverter : AppTypeConverter<ListId, Int> {
    @TypeConverter
    override fun toItem(v: Int): ListId = ListId(v)

    @TypeConverter
    override fun toEntity(v: ListId): Int = v.value
}

@Dao
abstract class ListDao {
    @Insert
    internal abstract suspend fun addList(entity: ListEntityDb): Long

    @Query("SELECT * FROM list WHERE rowid = :id")
    internal abstract suspend fun getListByRowId(id: Long): ListEntityDb

    @Query("SELECT * FROM list WHERE id = :id")
    internal abstract suspend fun getListById(id: ListId): ListEntityDb

    suspend fun addList(ownerId: UserId): ListId {
        val rowId = addList(ListEntityDb(ownerId = ownerId))
        return getListByRowId(rowId).id
    }

    @Query("DELETE FROM list WHERE id = :id")
    abstract suspend fun deleteList(id: ListId)

    @Query("SELECT * FROM list WHERE id = :id")
    abstract suspend fun findListEntityById(id: ListId): ListEntityDb?

    @Query("UPDATE list SET prepend_cursor = :cursor WHERE id = :id")
    abstract suspend fun updatePrependCursorById(id: ListId, cursor: Long?)

    @Query("UPDATE list SET append_cursor = :cursor WHERE id = :id")
    abstract suspend fun updateAppendCursorById(id: ListId, cursor: Long?)
}

internal suspend fun ListDao.updateCursorById(entities: PagedResponseList<*>, owner: ListId) {
    if (entities.prependCursor != null) {
        updatePrependCursorById(owner, entities.prependCursor)
    }
    if (entities.appendCursor != null) {
        updateAppendCursorById(owner, entities.appendCursor)
    }
    if (entities.prependCursor == null && entities.appendCursor == null) {
        updatePrependCursorById(owner, null)
        updateAppendCursorById(owner, null)
    }
}
