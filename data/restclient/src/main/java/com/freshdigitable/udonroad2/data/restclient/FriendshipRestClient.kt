package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import javax.inject.Inject

class FriendshipRestClient @Inject constructor(
    private val twitter: Twitter
) {
    suspend fun fetchFriendship(userId: UserId): Relationship = fetch {
        val sourceId = twitter.id
        val friendship = twitter.showFriendship(sourceId, userId.value)
        friendship.toEntity()
    }

    suspend fun createFriendship(userId: UserId): User = fetch {
        val user = twitter.createFriendship(userId.value)
        user.toEntity()
    }

    suspend fun destroyFriendship(userId: UserId): User = fetch {
        val user = twitter.destroyFriendship(userId.value)
        user.toEntity()
    }

    suspend fun createMute(userId: UserId): User = fetch {
        val user = twitter.createMute(userId.value)
        user.toEntity()
    }

    suspend fun destroyMute(userId: UserId): User = fetch {
        val user = twitter.destroyMute(userId.value)
        user.toEntity()
    }

    suspend fun createBlock(userId: UserId): User = fetch {
        val user = twitter.createBlock(userId.value)
        user.toEntity()
    }

    suspend fun destroyBlock(userId: UserId): User = fetch {
        val user = twitter.destroyBlock(userId.value)
        user.toEntity()
    }

    suspend fun updateFriendship(
        userId: UserId,
        notificationsEnabled: Boolean,
        wantRetweets: Boolean
    ): Relationship = fetch {
        val f = twitter.updateFriendship(userId.value, notificationsEnabled, wantRetweets)
        f.toEntity()
    }

    suspend fun reportSpam(userId: UserId): User = fetch {
        val user = twitter.reportSpam(userId.value)
        user.toEntity()
    }

    private suspend fun <T> fetch(block: CoroutineScope.() -> T): T = withContext(Dispatchers.IO) {
        block(this)
    }
}

private fun twitter4j.Relationship.toEntity(): Relationship {
    return RelationshipEntity(
        userId = UserId(this.targetUserId),
        following = this.isSourceFollowingTarget,
        blocking = this.isSourceBlockingTarget,
        muting = this.isSourceMutingTarget,
        wantRetweets = this.isSourceWantRetweets,
        notificationsEnabled = this.isSourceNotificationsEnabled
    )
}

private data class RelationshipEntity(
    override val userId: UserId,
    override val following: Boolean,
    override val blocking: Boolean,
    override val muting: Boolean,
    override val wantRetweets: Boolean,
    override val notificationsEnabled: Boolean
) : Relationship
