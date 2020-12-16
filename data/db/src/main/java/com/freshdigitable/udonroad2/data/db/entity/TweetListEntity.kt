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
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.freshdigitable.udonroad2.model.ListId
import com.freshdigitable.udonroad2.model.tweet.TweetId

@Entity(
    tableName = "tweet",
    foreignKeys = [
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["original_id"],
            deferred = true
        ),
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["body_item_id"],
            deferred = true
        )
    ],
    indices = [
        Index("body_item_id")
    ]
)
internal class TweetEntityDb(
    @PrimaryKey
    @ColumnInfo(name = "original_id")
    val originalId: TweetId,

    @ColumnInfo(name = "body_item_id")
    val bodyTweetId: TweetId,

    @ColumnInfo(name = "quoted_item_id")
    val quotedTweetId: TweetId?
)

@Entity(
    tableName = "tweet_list",
    primaryKeys = ["original_id", "list_id"],
    foreignKeys = [
        ForeignKey(
            entity = TweetEntityDb::class,
            parentColumns = ["original_id"],
            childColumns = ["original_id"],
            deferred = true
        ),
        ForeignKey(
            entity = ListEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = CASCADE
        ),
    ],
)
internal class TweetListEntity(
    @ColumnInfo(name = "original_id")
    val originalId: TweetId,

    @ColumnInfo(name = "list_id", index = true)
    val listId: ListId
)
