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
import androidx.room.Ignore
import androidx.room.Relation
import com.freshdigitable.udonroad2.data.db.entity.MediaEntity
import com.freshdigitable.udonroad2.data.db.entity.UrlEntity
import com.freshdigitable.udonroad2.data.db.entity.VideoValiantEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.UrlItem

@DatabaseView(
    """
    SELECT m.*, 
     r.tweet_id, 
     u.text AS url_text, u.display AS url_display, u.expanded AS url_expanded 
    FROM relation_tweet_media AS r 
    INNER JOIN media AS m ON r.media_id = m.id
    INNER JOIN url AS u ON m.url = u.text
""", viewName = "view_media"
)
internal data class MediaDbView(
    @ColumnInfo(name = "id")
    val id: MediaId,

    @ColumnInfo(name = "media_url")
    val mediaUrl: String,

    @Embedded(prefix = "url_")
    val url: UrlEntity,

    @ColumnInfo(name = "type")
    val type: MediaType,

    @Embedded(prefix = "large_")
    val largeSize: MediaEntity.Size,

    @Embedded(prefix = "medium_")
    val mediumSize: MediaEntity.Size,

    @Embedded(prefix = "small_")
    val smallSize: MediaEntity.Size,

    @Embedded(prefix = "thumb_")
    val thumbSize: MediaEntity.Size,

    @ColumnInfo(name = "video_aspect_ratio_width")
    val videoAspectRatioWidth: Int?,

    @ColumnInfo(name = "video_aspect_ratio_height")
    val videoAspectRatioHeight: Int?,

    @ColumnInfo(name = "video_duration_millis")
    val videoDurationMillis: Long?,

    @ColumnInfo(name = "tweet_id")
    val tweetId: Long
)

internal data class MediaItemDb(
    @Embedded
    val mediaDbView: MediaDbView
) : MediaItem {
    @Ignore
    override val id: MediaId = mediaDbView.id
    @Ignore
    override val mediaUrl: String = mediaDbView.mediaUrl
    @Ignore
    override val url: UrlItem = mediaDbView.url
    @Ignore
    override val type: MediaType = mediaDbView.type
    @Ignore
    override val largeSize: MediaItem.Size? = mediaDbView.largeSize
    @Ignore
    override val mediumSize: MediaItem.Size? = mediaDbView.mediumSize
    @Ignore
    override val smallSize: MediaItem.Size? = mediaDbView.smallSize
    @Ignore
    override val thumbSize: MediaItem.Size? = mediaDbView.thumbSize
    @Ignore
    override val videoAspectRatioWidth: Int? = mediaDbView.videoAspectRatioWidth
    @Ignore
    override val videoAspectRatioHeight: Int? = mediaDbView.videoAspectRatioHeight
    @Ignore
    override val videoDurationMillis: Long? = mediaDbView.videoDurationMillis

    @Relation(
        entity = VideoValiantEntity::class, entityColumn = "media_id", parentColumn = "id"
    )
    override var videoValiantItems: List<VideoValiantEntity> = listOf()
}
