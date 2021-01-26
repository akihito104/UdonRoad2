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
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.freshdigitable.udonroad2.model.MediaEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.TweetId

@Entity(
    tableName = "media_url",
    primaryKeys = ["id", "tweet_id"],
    foreignKeys = [
        ForeignKey(
            entity = MediaDbEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"]
        ),
        ForeignKey(
            entity = TweetElementDb::class,
            parentColumns = ["id"],
            childColumns = ["tweet_id"]
        )
    ]
)
internal data class MediaUrlEntity(
    @ColumnInfo(name = "id")
    val id: MediaId,

    @ColumnInfo(name = "tweet_id", index = true)
    val tweetId: TweetId,

    @ColumnInfo(name = "start")
    val start: Int,

    @ColumnInfo(name = "end")
    val end: Int,

    @ColumnInfo(name = "order")
    val order: Int,
) {
    companion object
}

@Entity(tableName = "media")
internal data class MediaDbEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: MediaId,

    // picture file url to get
    @ColumnInfo(name = "media_url")
    val mediaUrl: String,

    // t.co url included into tweet as a text
    @ColumnInfo(name = "url", index = true)
    val url: String,

    @ColumnInfo(name = "type")
    val type: MediaType,

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
    ) : MediaEntity.Size
}

@Entity(
    tableName = "video_valiant",
    primaryKeys = ["media_id", "url"],
    foreignKeys = [
        ForeignKey(
            entity = MediaDbEntity::class,
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
) : MediaEntity.VideoValiant {
    companion object
}

internal data class MediaEntityImpl(
    @Embedded
    private val entity: MediaDbEntity,
    @ColumnInfo(name = "start")
    override val start: Int,
    @ColumnInfo(name = "end")
    override val end: Int,
    @ColumnInfo(name = "order")
    override val order: Int,
) : MediaEntity {
    @Ignore
    override val largeSize: MediaEntity.Size? = entity.largeSize

    @Ignore
    override val mediumSize: MediaEntity.Size? = entity.mediumSize

    @Ignore
    override val smallSize: MediaEntity.Size? = entity.smallSize

    @Ignore
    override val thumbSize: MediaEntity.Size? = entity.thumbSize

    @Ignore
    override val videoAspectRatioWidth: Int? = entity.videoAspectRatioWidth

    @Ignore
    override val videoAspectRatioHeight: Int? = entity.videoAspectRatioHeight

    @Ignore
    override val videoDurationMillis: Long? = entity.videoDurationMillis

    @Ignore
    override val id: MediaId = entity.id

    @Ignore
    override val mediaUrl: String = entity.mediaUrl

    @Ignore
    override val url: String = entity.url

    @Ignore
    override val type: MediaType = entity.type

    @Relation(entity = VideoValiantEntity::class, entityColumn = "media_id", parentColumn = "id")
    override var videoValiantItems: List<VideoValiantEntity> = emptyList()
}
