package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.Relationship
import com.freshdigitable.udonroad2.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import javax.inject.Inject

class FriendshipRestClient @Inject constructor(
    private val twitter: Twitter
) {
    suspend fun fetchFriendship(userId: Long): Relationship = fetch {
        val sourceId = twitter.id
        val friendship = twitter.showFriendship(sourceId, userId)
        friendship.toEntity()
    }

    suspend fun createFriendship(userId: Long): User = fetch {
        val user = twitter.createFriendship(userId)
        user.toEntity()
    }

    suspend fun destroyFriendship(userId: Long): User = fetch {
        val user = twitter.destroyFriendship(userId)
        user.toEntity()
    }

    suspend fun createMute(userId: Long): User = fetch {
        val user = twitter.createMute(userId)
        user.toEntity()
    }

    suspend fun destroyMute(userId: Long): User = fetch {
        val user = twitter.destroyMute(userId)
        user.toEntity()
    }

    suspend fun createBlock(userId: Long): User = fetch {
        val user = twitter.createBlock(userId)
        user.toEntity()
    }

    suspend fun destroyBlock(userId: Long): User = fetch {
        val user = twitter.destroyBlock(userId)
        user.toEntity()
    }

    suspend fun updateFriendship(
        userId: Long,
        notificationsEnabled: Boolean,
        wantRetweets: Boolean
    ): Relationship = fetch {
        val f = twitter.updateFriendship(userId, notificationsEnabled, wantRetweets)
        f.toEntity()
    }

    suspend fun reportSpam(userId: Long): User = fetch {
        val user = twitter.reportSpam(userId)
        user.toEntity()
    }

    private suspend fun <T> fetch(block: CoroutineScope.() -> T): T = withContext(Dispatchers.IO) {
        block(this)
    }
}

private fun twitter4j.Relationship.toEntity(): Relationship {
    return RelationshipEntity(
        userId = this.targetUserId,
        following = this.isSourceFollowingTarget,
        blocking = this.isSourceBlockingTarget,
        muting = this.isSourceMutingTarget,
        wantRetweets = this.isSourceWantRetweets,
        notificationsEnabled = this.isSourceNotificationsEnabled
    )
}

private data class RelationshipEntity(
    override val userId: Long,
    override val following: Boolean,
    override val blocking: Boolean,
    override val muting: Boolean,
    override val wantRetweets: Boolean,
    override val notificationsEnabled: Boolean
) : Relationship
