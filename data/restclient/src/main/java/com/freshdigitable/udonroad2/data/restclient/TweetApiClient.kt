package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import twitter4j.StatusUpdate
import java.io.InputStream
import javax.inject.Inject

class TweetApiClient @Inject constructor(
    private val twitter: AppTwitter
) {
    suspend fun fetchTweet(id: TweetId): TweetEntity = twitter.fetch {
        showStatus(id.value).toEntity()
    }

    suspend fun postLike(id: TweetId): TweetEntity = twitter.fetch {
        createFavorite(id.value).toEntity()
    }

    suspend fun postRetweet(id: TweetId): TweetEntity = twitter.fetch {
        retweetStatus(id.value).toEntity()
    }

    suspend fun postTweet(text: String, mediaIds: List<MediaId>): TweetEntity = twitter.fetch {
        if (mediaIds.isEmpty()) {
            updateStatus(text).toEntity()
        } else {
            val status = StatusUpdate(text).apply {
                setMediaIds(*mediaIds.map { it.value }.toLongArray())
            }
            updateStatus(status).toEntity()
        }
    }

    suspend fun uploadMedia(filename: String, inputStream: InputStream): MediaId = twitter.fetch {
        val uploadedMedia = uploadMedia(filename, inputStream)
        MediaId(uploadedMedia.mediaId)
    }
}
