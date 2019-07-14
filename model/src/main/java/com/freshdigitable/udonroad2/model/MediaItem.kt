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

package com.freshdigitable.udonroad2.model

interface MediaItem {
    val id: MediaId

    val mediaUrl: String

    val url: UrlItem

    val type: String

    val largeSize: Size

    val mediumSize: Size

    val smallSize: Size

    val thumbSize: Size

    val videoAspectRatioWidth: Int?

    val videoAspectRatioHeight: Int?

    val videoDurationMillis: Long?

    interface Size {
        val type: Int

        val width: Int

        val height: Int

        val resizeType: Int
    }
}

data class MediaId(val value: Long)
