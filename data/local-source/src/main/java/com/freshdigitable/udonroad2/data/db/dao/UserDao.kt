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
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freshdigitable.udonroad2.data.UserRepository
import com.freshdigitable.udonroad2.data.db.dbview.UserListDbView
import com.freshdigitable.udonroad2.data.db.entity.UserEntityDb
import com.freshdigitable.udonroad2.data.db.entity.UserListEntity
import com.freshdigitable.udonroad2.data.db.ext.toEntity
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addUsers(users: List<UserEntityDb>)

    @Query("SELECT * FROM user WHERE id = :id")
    internal abstract fun getUserSource(id: UserId): LiveData<UserEntityDb?>

    @Query("SELECT * FROM user WHERE id = :id")
    internal abstract suspend fun getUser(id: UserId): UserEntityDb?

    @Query("SELECT * FROM user WHERE id = :id")
    internal abstract fun getUserFlow(id: UserId): Flow<UserEntityDb?>

    @Query(
        """
        SELECT i.*
        FROM user_list AS l
        INNER JOIN view_user_item AS i ON l.user_id = i.id
        WHERE list_id = :owner
        ORDER BY l.id"""
    )
    internal abstract fun getUserList(owner: ListId): PagingSource<Int, UserListDbView>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addUserListEntities(entities: List<UserListEntity>)

    @Query("DELETE FROM user_list WHERE list_id = :owner")
    abstract suspend fun deleteByListId(owner: ListId)
}

internal class UserLocalSource(private val dao: UserDao) : UserRepository.LocalSource {
    override fun getUserSource(id: UserId): Flow<UserEntity?> = dao.getUserFlow(id)

    override suspend fun findUser(id: UserId): UserEntity? = dao.getUser(id)

    override suspend fun getUser(id: UserId): UserEntity =
        findUser(id) ?: throw IllegalStateException("UserId: $id is not found in local...")

    override suspend fun addUser(user: UserEntity) {
        addUsers(listOf(user))
    }

    override suspend fun addUsers(users: List<UserEntity>) {
        val u = users.map { user ->
            when (user) {
                is UserEntityDb -> user
                else -> user.toEntity()
            }
        }
        dao.addUsers(u)
    }
}
