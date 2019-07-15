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

package com.freshdigitable.udonroad2.data.restclient.data

import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.MediaItem
import com.freshdigitable.udonroad2.model.UrlItem

internal data class MediaItemRest(
    override val id: MediaId,
    override val mediaUrl: String,
    override val url: UrlItem,
    override val type: String,
    override val largeSize: MediaItem.Size?,
    override val mediumSize: MediaItem.Size?,
    override val smallSize: MediaItem.Size?,
    override val thumbSize: MediaItem.Size?,
    override val videoAspectRatioWidth: Int?,
    override val videoAspectRatioHeight: Int?,
    override val videoDurationMillis: Long?
): MediaItem

internal data class SizeRest(
    override val width: Int,
    override val height: Int,
    override val resizeType: Int
) : MediaItem.Size
