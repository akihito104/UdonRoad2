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
import androidx.room.Ignore
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaType
import com.freshdigitable.udonroad2.model.TweetMediaItem
import com.freshdigitable.udonroad2.model.UrlItem
import com.freshdigitable.udonroad2.model.tweet.TweetId

@DatabaseView(
    """
    SELECT r.*, m.media_url, m.url, m.type
    FROM media_url AS r 
    INNER JOIN media AS m ON r.id = m.id
""",
    viewName = "view_tweet_item_media"
)
internal data class MediaDbView(
    @ColumnInfo(name = "id")
    override val id: MediaId,

    @ColumnInfo(name = "media_url")
    override val mediaUrl: String,

    @ColumnInfo(name = "url")
    private val _url: String,

    @ColumnInfo(name = "type")
    override val type: MediaType,

    @ColumnInfo(name = "tweet_id")
    val tweetId: TweetId
) : TweetMediaItem {
    @Ignore
    override val url: UrlItem = object : UrlItem {
        override val displayUrl: String
            get() = ""
        override val expandedUrl: String = _url
        override val text: String = _url
    }
}

