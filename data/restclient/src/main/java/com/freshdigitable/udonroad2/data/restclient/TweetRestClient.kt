package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.TweetEntity
import twitter4j.Twitter
import javax.inject.Inject

class TweetApiClient @Inject constructor(
    private val twitter: Twitter
) {
    fun fetchTweet(id: Long): TweetEntity {
        return twitter.showStatus(id).toEntity()
    }
}
