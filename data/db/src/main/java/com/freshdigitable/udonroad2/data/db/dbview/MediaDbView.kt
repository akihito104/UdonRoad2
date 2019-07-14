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

package com.freshdigitable.udonroad2.data.db.dbview

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Embedded
import com.freshdigitable.udonroad2.data.db.entity.MediaEntity
import com.freshdigitable.udonroad2.data.db.entity.UrlEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaItem

@DatabaseView("""
    SELECT m.*, 
     r.tweet_id, 
     u.text AS url_text, u.display AS url_display, u.expanded AS url_expanded 
    FROM relation_tweet_media AS r 
    INNER JOIN media AS m ON r.media_id = m.id
    INNER JOIN url AS u ON m.url = u.text
""", viewName = "view_media")
internal data class MediaDbView(
    @ColumnInfo(name = "id")
    override val id: MediaId,

    @ColumnInfo(name = "media_url")
    override val mediaUrl: String,

    @Embedded(prefix = "url_")
    override val url: UrlEntity,

    @ColumnInfo(name = "type")
    override val type: String,

    @Embedded(prefix = "large_")
    override val largeSize: MediaEntity.Size,

    @Embedded(prefix = "medium_")
    override val mediumSize: MediaEntity.Size,

    @Embedded(prefix = "small_")
    override val smallSize: MediaEntity.Size,

    @Embedded(prefix = "thumb_")
    override val thumbSize: MediaEntity.Size,

    @ColumnInfo(name = "video_aspect_ratio_width")
    override val videoAspectRatioWidth: Int?,

    @ColumnInfo(name = "video_aspect_ratio_height")
    override val videoAspectRatioHeight: Int?,

    @ColumnInfo(name = "video_duration_millis")
    override val videoDurationMillis: Long?,

    @ColumnInfo(name = "tweet_id")
    val tweetId: Long

): MediaItem
