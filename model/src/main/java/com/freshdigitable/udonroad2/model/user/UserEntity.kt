/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.model.user

import java.io.Serializable

interface TweetUserItem : Serializable {

    val id: UserId

    val name: String

    val screenName: String

    val iconUrl: String

    val isVerified: Boolean

    val isProtected: Boolean

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface UserListItem : TweetUserItem {
    val description: String
    val followerCount: Int
    val followingCount: Int

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface UserEntity : UserListItem {
    val profileBannerImageUrl: String?
    val tweetCount: Int
    val favoriteCount: Int
    val listedCount: Int
    val profileLinkColor: Int
    val location: String
    val url: String?
}

data class UserId(val value: Long) : Serializable {
    val isValid: Boolean = value >= 0

    companion object {
        fun create(value: Long?): UserId? = value?.let { UserId(it) }
    }
}
