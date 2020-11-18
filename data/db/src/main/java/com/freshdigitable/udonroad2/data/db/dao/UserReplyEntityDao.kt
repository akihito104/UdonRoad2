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

package com.freshdigitable.udonroad2.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.db.entity.UserReplyEntityDb
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity

@Dao
abstract class UserReplyEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun addEntities(entities: List<UserReplyEntityDb>)

    @Query("SELECT * FROM user_reply WHERE tweet_id = :id")
    internal abstract suspend fun findEntitiesByTweetId(id: TweetId): List<UserReplyEntityDb>
}

internal class ReplyLocalDataSource(
    private val dao: UserReplyEntityDao
) : ReplyRepository.LocalSource {
    override suspend fun findEntitiesByTweetId(id: TweetId): List<UserReplyEntity> {
        return dao.findEntitiesByTweetId(id)
    }
}
