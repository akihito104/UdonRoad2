package com.freshdigitable.udonroad2.data.db.ext

import com.freshdigitable.udonroad2.data.db.entity.UserEntity
import com.freshdigitable.udonroad2.model.User

internal fun User.toEntity(): UserEntity {
    return if (this is UserEntity) {
        return this
    } else {
        UserEntity(
            id,
            name,
            screenName,
            iconUrl,
            description,
            profileBannerImageUrl,
            followerCount,
            followingCount,
            tweetCount,
            favoriteCount,
            listedCount,
            profileLinkColor,
            location,
            url,
            verified,
            isProtected
        )
    }
}
