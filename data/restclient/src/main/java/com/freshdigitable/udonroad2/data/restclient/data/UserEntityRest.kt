package com.freshdigitable.udonroad2.data.restclient.data

import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.model.user.UserId

internal data class UserEntityRest(
    override val id: UserId,
    override val name: String,
    override val screenName: String,
    override val iconUrl: String,
    override val description: String,
    override val profileBannerImageUrl: String?,
    override val followerCount: Int,
    override val followingCount: Int,
    override val tweetCount: Int,
    override val favoriteCount: Int,
    override val listedCount: Int,
    override val profileLinkColor: Int,
    override val location: String,
    override val url: String?,
    override val isVerified: Boolean,
    override val isProtected: Boolean
) : UserEntity
