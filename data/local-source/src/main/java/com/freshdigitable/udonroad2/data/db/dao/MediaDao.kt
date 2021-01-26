/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.entity.MediaDbEntity
import com.freshdigitable.udonroad2.data.db.entity.MediaEntityImpl
import com.freshdigitable.udonroad2.data.db.entity.MediaUrlEntity
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.TweetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class MediaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addMediaEntities(entities: Iterable<MediaDbEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addTweetMediaRelations(rels: Iterable<MediaUrlEntity>)

    @Transaction
    @Query(
        """
            SELECT media.*, media_url.start, media_url.`end`, media_url.`order`
             FROM media_url
             INNER JOIN media ON media.id = media_url.id
             WHERE tweet_id = :tweetId 
        """
    )
    internal abstract fun getMediaSourceByTweetId(tweetId: TweetId): Flow<List<MediaEntityImpl>>

    fun getMediaItemSource(tweetId: TweetId): Flow<List<MediaEntity>> =
        getMediaSourceByTweetId(tweetId).map { i -> i.sortedBy { it.order } }
}

@Dao
abstract class VideoValiantDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addVideoValiantEntities(entities: Iterable<VideoValiantEntity>)
}
