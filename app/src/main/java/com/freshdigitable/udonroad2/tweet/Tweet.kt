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

package com.freshdigitable.udonroad2.tweet

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.freshdigitable.udonroad2.user.User
import org.threeten.bp.Instant

data class Tweet(
        @ColumnInfo(name = "id")
        val id: Long,

        @ColumnInfo(name = "text")
        val text: String,

        @ColumnInfo(name = "retweet_count")
        val retweetCount: Int,

        @ColumnInfo(name = "favorite_count")
        val favoriteCount: Int,

        @Embedded(prefix = "user_")
        val user: User,

        @ColumnInfo(name = "source")
        val source: String,

        @ColumnInfo(name = "created_at")
        val createdAt: Instant
)

data class TweetListItem(
        @ColumnInfo(name = "original_id")
        val originalId: Long,
        @Embedded(prefix = "original_user_")
        val originalUser: User,

        @Embedded
        val body: Tweet,

        @Embedded(prefix = "qt_")
        val quoted: Tweet?
) {
    fun isRetweet(): Boolean = originalId != body.id
}
