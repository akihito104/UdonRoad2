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
import com.freshdigitable.udonroad2.data.db.entity.MediaEntity
import com.freshdigitable.udonroad2.data.db.entity.MediaItemDb
import com.freshdigitable.udonroad2.data.db.entity.MediaUrlEntity
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.tweet.TweetId
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MediaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addMediaEntities(entities: Iterable<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addTweetMediaRelations(rels: Iterable<MediaUrlEntity>)

    @Transaction
    @Query(
        """
            SELECT media.* 
             FROM media_url
             INNER JOIN media ON media.id = media_url.id
             WHERE tweet_id = :tweetId 
        """
    )
    internal abstract fun getMediaSourceByTweetId(tweetId: TweetId): Flow<List<MediaItemDb>>

    fun getMediaItemSource(tweetId: TweetId): Flow<List<MediaItem>> =
        getMediaSourceByTweetId(tweetId)
}

@Dao
abstract class VideoValiantDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract suspend fun addVideoValiantEntities(entities: Iterable<VideoValiantEntity>)
}
