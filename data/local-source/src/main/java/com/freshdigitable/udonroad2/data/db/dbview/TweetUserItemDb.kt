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
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.TweetUserItem

internal data class TweetUserItemDb(
    @ColumnInfo(name = "id")
    override val id: UserId,

    @ColumnInfo(name = "name")
    override val name: String,

    @ColumnInfo(name = "screen_name")
    override val screenName: String,

    @ColumnInfo(name = "icon_url")
    override val iconUrl: String,

    @ColumnInfo(name = "is_verified")
    override val isVerified: Boolean,

    @ColumnInfo(name = "is_protected")
    override val isProtected: Boolean,
) : TweetUserItem
