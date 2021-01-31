package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.TweetDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.tweet.DetailTweetListItem
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetEntityUpdatable
import kotlinx.coroutines.flow.Flow
import twitter4j.StatusUpdate
import java.io.InputStream
import javax.inject.Inject

class TweetApiClient @Inject constructor(
    private val twitter: AppTwitter
) : TweetDataSource.Remote {
    override suspend fun findTweetEntity(tweetId: TweetId): TweetEntity = twitter.fetch {
        showStatus(tweetId.value).toEntity()
    }

    override suspend fun updateLike(id: TweetId, isLiked: Boolean): TweetEntityUpdatable {
        return twitter.fetch {
            when (isLiked) {
                true -> createFavorite(id.value)
                false -> destroyFavorite(id.value)
            }
        }.toEntity()
    }

    override suspend fun updateRetweet(id: TweetId, isRetweeted: Boolean): TweetEntityUpdatable {
        return when (isRetweeted) {
            true -> postRetweet(id)
            else -> postUnretweet(id)
        }
    }

    private suspend fun postRetweet(id: TweetId): TweetEntity = twitter.fetch {
        retweetStatus(id.value).toEntity()
    }

    private suspend fun postUnretweet(id: TweetId): TweetEntity = twitter.fetch {
        unRetweetStatus(id.value).toEntity()
    }

    override suspend fun deleteTweet(id: TweetId): Unit = twitter.fetch {
        destroyStatus(id.value)
    }

    suspend fun postTweet(
        text: String,
        mediaIds: List<MediaId> = emptyList(),
        replyTo: TweetId? = null,
    ): TweetEntity = twitter.fetch {
        if (mediaIds.isEmpty()) {
            updateStatus(text).toEntity()
        } else {
            val status = StatusUpdate(text).apply {
                setMediaIds(*mediaIds.map { it.value }.toLongArray())
                replyTo?.let { inReplyToStatusId = it.value }
            }
            updateStatus(status).toEntity()
        }
    }

    suspend fun uploadMedia(filename: String, inputStream: InputStream): MediaId = twitter.fetch {
        val uploadedMedia = uploadMedia(filename, inputStream)
        MediaId(uploadedMedia.mediaId)
    }

    override suspend fun addTweetEntity(tweet: TweetEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun findDetailTweetItem(id: TweetId): DetailTweetListItem =
        throw NotImplementedError()

    override fun getDetailTweetItemSource(id: TweetId): Flow<DetailTweetListItem?> =
        throw NotImplementedError()

    override suspend fun updateTweet(tweet: TweetEntityUpdatable) = throw NotImplementedError()
}
