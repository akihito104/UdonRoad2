package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.User

class UserEntityRest(
    override val id: Long,
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
    override val verified: Boolean,
    override val isProtected: Boolean
) : User
