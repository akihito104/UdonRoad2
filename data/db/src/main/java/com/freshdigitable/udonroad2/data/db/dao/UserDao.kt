/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.db.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
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
import com.freshdigitable.udonroad2.data.db.dbview.UserListDbView
import com.freshdigitable.udonroad2.data.db.entity.UserEntity
import com.freshdigitable.udonroad2.model.User

@Dao
abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addUsers(users: List<UserEntity>)

    @Query("SELECT * FROM UserEntity WHERE id = :id")
    internal abstract fun getUser(id: Long): LiveData<UserEntity?>

    open fun getUserById(id: Long): LiveData<User?> = getUser(id).map { it }

    @Transaction
    internal open suspend fun addUsers(entities: List<UserEntity>, owner: String? = null) {
        addUsers(entities)
        if (owner != null) {
            val listEntities = entities.map {
                UserListEntity(userId = it.id, owner = owner)
            }
            addUserListEntities(listEntities)
        }
    }

    @Query(
        """
        SELECT i.*
        FROM user_list AS l
        INNER JOIN user_list_item AS i ON l.user_id = i.id
        WHERE owner = :owner
        ORDER BY l.id"""
    )
    internal abstract fun getUserList(owner: String): DataSource.Factory<Int, UserListDbView>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addUserListEntities(entities: List<UserListEntity>)

    @Query("DELETE FROM user_list WHERE owner = :owner")
    abstract suspend fun clear(owner: String)
}

@Entity(
    tableName = "user_list",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"]
        )
    ]
)
internal data class UserListEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "user_id", index = true)
    val userId: Long,

    @ColumnInfo(name = "owner")
    val owner: String
)
