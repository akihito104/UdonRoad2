package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.data.RelationDataSource
import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FriendshipRestClient @Inject constructor(
    private val twitter: AppTwitter
) : RelationDataSource.Remote {
    override suspend fun findRelationship(targetUserId: UserId): Relationship = twitter.fetch {
        showFriendship(id, targetUserId.value).toEntity()
    }

    override suspend fun updateFollowingStatus(
        targetUserId: UserId,
        isFollowing: Boolean
    ): UserEntity = twitter.fetch {
        when (isFollowing) {
            true -> createFriendship(targetUserId.value)
            false -> destroyFriendship(targetUserId.value)
        }
    }.toEntity()

    override suspend fun updateMutingStatus(targetUserId: UserId, isMuting: Boolean): UserEntity =
        twitter.fetch {
            when (isMuting) {
                true -> createMute(targetUserId.value)
                false -> destroyMute(targetUserId.value)
            }
        }.toEntity()

    override suspend fun updateBlockingStatus(
        targetUserId: UserId,
        isBlocking: Boolean
    ): UserEntity = twitter.fetch {
        when (isBlocking) {
            true -> createBlock(targetUserId.value)
            false -> destroyBlock(targetUserId.value)
        }
    }.toEntity()

    override suspend fun updateRelationship(relationship: Relationship) {
        with(relationship) {
            updateFriendship(sourceUserId, notificationsEnabled, wantRetweets)
        }
    }

    private suspend fun updateFriendship(
        userId: UserId,
        notificationsEnabled: Boolean,
        wantRetweets: Boolean
    ): Relationship = twitter.fetch {
        updateFriendship(userId.value, notificationsEnabled, wantRetweets).toEntity()
    }

    override suspend fun addSpam(userId: UserId): UserEntity = twitter.fetch {
        reportSpam(userId.value).toEntity()
    }

    override fun getRelationshipSource(targetUserId: UserId): Flow<Relationship?> =
        throw NotImplementedError()

    override suspend fun updateWantRetweetStatus(
        targetUserId: UserId,
        wantRetweets: Boolean
    ): Relationship = throw NotImplementedError()
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
