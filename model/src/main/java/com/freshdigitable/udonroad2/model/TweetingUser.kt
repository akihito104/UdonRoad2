package com.freshdigitable.udonroad2.model

import java.io.Serializable

interface TweetingUser : Serializable {

    val id: Long

    val name: String

    val screenName: String

    val iconUrl: String
}

interface UserListItem : TweetingUser {
    val description: String
    val followerCount: Int
    val followingCount: Int
    val verified: Boolean
    val isProtected: Boolean
}

interface User : UserListItem {
    val profileBannerImageUrl: String?
    val tweetCount: Int
    val favoriteCount: Int
    val listedCount: Int
    val profileLinkColor: Int
    val location: String
    val url: String?
}
