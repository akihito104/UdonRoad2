package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.TweetEntity
import com.freshdigitable.udonroad2.model.TweetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import javax.inject.Inject

class TweetApiClient @Inject constructor(
    private val twitter: Twitter
) {
    suspend fun fetchTweet(id: TweetId): TweetEntity = withContext(Dispatchers.IO) {
        twitter.showStatus(id.value).toEntity()
    }
}
