package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import javax.inject.Inject

class FriendshipRestClient @Inject constructor(
    private val twitter: AppTwitter
) {
    suspend fun fetchFriendship(userId: UserId): Relationship = twitter.fetch {
        showFriendship(id, userId.value).toEntity()
    }

    suspend fun createFriendship(userId: UserId): User = twitter.fetch {
        createFriendship(userId.value).toEntity()
    }

    suspend fun destroyFriendship(userId: UserId): User = twitter.fetch {
        destroyFriendship(userId.value).toEntity()
    }

    suspend fun createMute(userId: UserId): User = twitter.fetch {
        createMute(userId.value).toEntity()
    }

    suspend fun destroyMute(userId: UserId): User = twitter.fetch {
        destroyMute(userId.value).toEntity()
    }

    suspend fun createBlock(userId: UserId): User = twitter.fetch {
        createBlock(userId.value).toEntity()
    }

    suspend fun destroyBlock(userId: UserId): User = twitter.fetch {
        destroyBlock(userId.value).toEntity()
    }

    suspend fun updateFriendship(
        userId: UserId,
        notificationsEnabled: Boolean,
        wantRetweets: Boolean
    ): Relationship = twitter.fetch {
        updateFriendship(userId.value, notificationsEnabled, wantRetweets).toEntity()
    }

    suspend fun reportSpam(userId: UserId): User = twitter.fetch {
        reportSpam(userId.value).toEntity()
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
