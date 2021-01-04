package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserEntity
import javax.inject.Inject

class FriendshipRestClient @Inject constructor(
    private val twitter: AppTwitter
) {
    suspend fun fetchFriendship(userId: UserId): Relationship = twitter.fetch {
        showFriendship(id, userId.value).toEntity()
    }

    suspend fun createFriendship(userId: UserId): UserEntity = twitter.fetch {
        createFriendship(userId.value).toEntity()
    }

    suspend fun destroyFriendship(userId: UserId): UserEntity = twitter.fetch {
        destroyFriendship(userId.value).toEntity()
    }

    suspend fun createMute(userId: UserId): UserEntity = twitter.fetch {
        createMute(userId.value).toEntity()
    }

    suspend fun destroyMute(userId: UserId): UserEntity = twitter.fetch {
        destroyMute(userId.value).toEntity()
    }

    suspend fun createBlock(userId: UserId): UserEntity = twitter.fetch {
        createBlock(userId.value).toEntity()
    }

    suspend fun destroyBlock(userId: UserId): UserEntity = twitter.fetch {
        destroyBlock(userId.value).toEntity()
    }

    suspend fun updateFriendship(
        userId: UserId,
        notificationsEnabled: Boolean,
        wantRetweets: Boolean
    ): Relationship = twitter.fetch {
        updateFriendship(userId.value, notificationsEnabled, wantRetweets).toEntity()
    }

    suspend fun reportSpam(userId: UserId): UserEntity = twitter.fetch {
        reportSpam(userId.value).toEntity()
    }
}

private fun twitter4j.Relationship.toEntity(): Relationship {
    return RelationshipEntity(
        targetUserId = UserId(this.targetUserId),
        following = this.isSourceFollowingTarget,
        blocking = this.isSourceBlockingTarget,
        muting = this.isSourceMutingTarget,
        wantRetweets = this.isSourceWantRetweets,
        notificationsEnabled = this.isSourceNotificationsEnabled,
        sourceUserId = UserId(this.sourceUserId),
    )
}

private data class RelationshipEntity(
    override val targetUserId: UserId,
    override val following: Boolean,
    override val blocking: Boolean,
    override val muting: Boolean,
    override val wantRetweets: Boolean,
    override val notificationsEnabled: Boolean,
    override val sourceUserId: UserId,
) : Relationship
