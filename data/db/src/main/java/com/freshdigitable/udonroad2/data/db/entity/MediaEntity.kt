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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.TweetId

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = UrlEntity::class,
            parentColumns = ["text"],
            childColumns = ["url"]
        )
    ]
)
internal data class MediaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: MediaId,

    @ColumnInfo(name = "media_url")
    val mediaUrl: String,

    @ColumnInfo(name = "url", index = true)
    val url: String,

    @ColumnInfo(name = "type")
    val type: String,

    @Embedded(prefix = "large_")
    val largeSize: Size?,

    @Embedded(prefix = "medium_")
    val mediumSize: Size?,

    @Embedded(prefix = "small_")
    val smallSize: Size?,

    @Embedded(prefix = "thumb_")
    val thumbSize: Size?,

    @ColumnInfo(name = "video_aspect_ratio_width")
    val videoAspectRatioWidth: Int?,

    @ColumnInfo(name = "video_aspect_ratio_height")
    val videoAspectRatioHeight: Int?,

    @ColumnInfo(name = "video_duration_millis")
    val videoDurationMillis: Long?
) {
    data class Size(
        @ColumnInfo(name = "width")
        override val width: Int,

        @ColumnInfo(name = "height")
        override val height: Int,

        @ColumnInfo(name = "resize_type")
        override val resizeType: Int
    ) : MediaItem.Size
}

@Entity(
    tableName = "video_valiant",
    primaryKeys = ["media_id", "url"],
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"]
        )
    ]
)
internal data class VideoValiantEntity(
    @ColumnInfo(name = "media_id")
    val mediaId: MediaId,

    @ColumnInfo(name = "url")
    override val url: String,

    @ColumnInfo(name = "bitrate")
    override val bitrate: Int,

    @ColumnInfo(name = "content_type")
    override val contentType: String
) : MediaItem.VideoValiant

@Entity(
    tableName = "relation_tweet_media",
    primaryKeys = ["tweet_id", "media_id"],
    foreignKeys = [
        ForeignKey(
            entity = TweetEntityDb::class,
            parentColumns = ["id"],
            childColumns = ["tweet_id"]
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"]
        )
    ]
)
internal data class TweetMediaRelation(
    @ColumnInfo(name = "tweet_id")
    val tweetId: TweetId,

    @ColumnInfo(name = "media_id", index = true)
    val mediaId: MediaId
)
