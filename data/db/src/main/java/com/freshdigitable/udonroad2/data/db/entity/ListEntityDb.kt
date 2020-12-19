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
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import com.freshdigitable.udonroad2.data.db.AppTypeConverter
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.user.UserId

@Entity(
    tableName = "list",
    // FIXME: redesign launch sequence
//    foreignKeys = [
//        ForeignKey(
//            entity = UserEntityDb::class,
//            parentColumns = ["id"],
//            childColumns = ["owner_id"]
//        )
//    ]
)
data class ListEntityDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int? = null,
    @ColumnInfo(name = "owner_id", index = true)
    val ownerId: UserId,
)

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

    suspend fun addList(ownerId: UserId): ListId {
        val rowId = addList(ListEntityDb(ownerId = ownerId))
        return ListId(getListByRowId(rowId).id!!)
    }

    @Query("DELETE FROM list WHERE id = :id")
    abstract suspend fun deleteList(id: ListId)
}
