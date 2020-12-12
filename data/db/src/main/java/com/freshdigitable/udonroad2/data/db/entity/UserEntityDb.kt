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

package com.freshdigitable.udonroad2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserId

@Entity(tableName = "user")
internal data class UserEntityDb(
    @PrimaryKey
    @ColumnInfo(name = "id")
    override val id: UserId,

    @ColumnInfo(name = "name")
    override val name: String,

    @ColumnInfo(name = "screen_name")
    override val screenName: String,

    @ColumnInfo(name = "icon_url")
    override val iconUrl: String,

    @ColumnInfo(name = "description")
    override val description: String,

    @ColumnInfo(name = "profile_banner_image_url")
    override val profileBannerImageUrl: String?,

    @ColumnInfo(name = "follower_count")
    override val followerCount: Int,

    @ColumnInfo(name = "following_count")
    override val followingCount: Int,

    @ColumnInfo(name = "tweet_count")
    override val tweetCount: Int,

    @ColumnInfo(name = "favorite_count")
    override val favoriteCount: Int,

    @ColumnInfo(name = "listed_count")
    override val listedCount: Int,

    @ColumnInfo(name = "profile_link_color")
    override val profileLinkColor: Int,

    @ColumnInfo(name = "location")
    override val location: String,

    @ColumnInfo(name = "url")
    override val url: String?,

    @ColumnInfo(name = "is_verified")
    override val isVerified: Boolean,

    @ColumnInfo(name = "is_protected")
    override val isProtected: Boolean
) : UserEntity {

    constructor(user: UserEntity) : this(
        id = user.id,
        name = user.name,
        screenName = user.screenName,
        description = user.description,
        iconUrl = user.iconUrl,
        profileBannerImageUrl = user.profileBannerImageUrl,
        followerCount = user.followerCount,
        followingCount = user.followingCount,
        tweetCount = user.tweetCount,
        favoriteCount = user.favoriteCount,
        listedCount = user.listedCount,
        profileLinkColor = user.profileLinkColor,
        location = user.location,
        url = user.url,
        isVerified = user.isVerified,
        isProtected = user.isProtected
    )
}
