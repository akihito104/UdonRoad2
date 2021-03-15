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

package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UrlItem

@Entity(tableName = "url_tweet", primaryKeys = ["tweet_id", "url"])
internal data class UrlEntity(
    @ColumnInfo(name = "tweet_id")
    val id: TweetId,

    @ColumnInfo(name = "url", index = true)
    override val url: String,

    @ColumnInfo(name = "display")
    override val displayUrl: String,

    @ColumnInfo(name = "expanded")
    override val expandedUrl: String,

    @ColumnInfo(name = "start")
    override val start: Int,

    @ColumnInfo(name = "end")
    override val end: Int,
) : UrlItem
