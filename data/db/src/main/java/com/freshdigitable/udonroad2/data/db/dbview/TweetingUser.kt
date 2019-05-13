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
import androidx.room.DatabaseView
import com.freshdigitable.udonroad2.model.TweetingUser

@DatabaseView(viewName = "view_user_in_tweet", value = """
    SELECT id, name, screen_name, icon_url FROM UserEntity
""")
data class TweetingUser(
    @ColumnInfo(name = "id")
    override val id: Long,

    @ColumnInfo(name = "name")
    override val name: String,

    @ColumnInfo(name = "screen_name")
    override val screenName: String,

    @ColumnInfo(name = "icon_url")
    override val iconUrl: String
) : TweetingUser
