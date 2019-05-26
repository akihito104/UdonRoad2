package com.freshdigitable.udonroad2.model

interface TweetingUser {

    val id: Long

    val name: String

    val screenName: String

    val iconUrl: String
}

interface User : TweetingUser {
    val description: String
    val profileBannerImageUrl: String?
    val followerCount: Int
    val followingCount: Int
    val tweetCount: Int
    val favoriteCount: Int
    val listedCount: Int
    val profileLinkColor: Int
    val location: String
    val url: String?
    val verified: Boolean
    val isProtected: Boolean
}
