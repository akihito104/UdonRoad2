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

package com.freshdigitable.udonroad2.data.db.dbview

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.freshdigitable.udonroad2.model.TweetListItem
import org.threeten.bp.Instant

data class Tweet(
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "text")
    override val text: String,

    @ColumnInfo(name = "retweet_count")
    override val retweetCount: Int,

    @ColumnInfo(name = "favorite_count")
    override val favoriteCount: Int,

    @Embedded(prefix = "user_")
    override val user: User,

    @ColumnInfo(name = "source")
    override val source: String,

    @ColumnInfo(name = "created_at")
    override val createdAt: Instant
): com.freshdigitable.udonroad2.model.Tweet

data class TweetListItem(
    @ColumnInfo(name = "original_id")
    override val originalId: Long,

    @Embedded(prefix = "original_user_")
    override val originalUser: User,

    @Embedded
    override val body: Tweet,

    @Embedded(prefix = "qt_")
    override val quoted: Tweet?
) : TweetListItem
