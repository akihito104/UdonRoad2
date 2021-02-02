package com.freshdigitable.udonroad2.data.impl

import com.freshdigitable.udonroad2.data.RelationDataSource
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserEntity

class RelationshipRepository(
    private val dao: RelationDataSource.Local,
    private val restClient: RelationDataSource.Remote,
) : RelationDataSource by dao {

    override suspend fun findRelationship(targetUserId: UserId): Relationship? {
        val f = restClient.findRelationship(targetUserId)
        f?.let { dao.updateRelationship(it) }
        return f
    }

    override suspend fun updateFollowingStatus(
        targetUserId: UserId,
        isFollowing: Boolean
    ): UserEntity {
        try {
            val user = restClient.updateFollowingStatus(targetUserId, isFollowing)
            return dao.updateFollowingStatus(user.id, isFollowing)
        } catch (e: AppTwitterException) {
            findRelationship(targetUserId)
            throw e
        }
    }

    override suspend fun updateMutingStatus(targetUserId: UserId, isMuting: Boolean): UserEntity {
        try {
            val user = restClient.updateMutingStatus(targetUserId, isMuting)
            return dao.updateMutingStatus(user.id, isMuting)
        } catch (e: AppTwitterException) {
            findRelationship(targetUserId)
            throw e
        }
    }

    override suspend fun updateBlockingStatus(
        targetUserId: UserId,
        isBlocking: Boolean
    ): UserEntity {
        try {
            val user = restClient.updateBlockingStatus(targetUserId, isBlocking)
            return dao.updateBlockingStatus(user.id, isBlocking)
        } catch (e: AppTwitterException) {
            findRelationship(targetUserId)
            throw e
        }
    }

    override suspend fun updateWantRetweetStatus(
        targetUserId: UserId,
        wantRetweets: Boolean
    ): Relationship {
        try {
            val currentRelation = requireNotNull(restClient.findRelationship(targetUserId))
            val rel = object : Relationship by currentRelation {
                override val wantRetweets: Boolean
                    get() = wantRetweets
            }
            restClient.updateRelationship(rel)
            return dao.updateWantRetweetStatus(rel.targetUserId, rel.wantRetweets)
        } catch (e: AppTwitterException) {
            findRelationship(targetUserId)
            throw e
        }
    }

    override suspend fun addSpam(userId: UserId): UserEntity {
        val user = restClient.addSpam(userId)
        return dao.addSpam(user.id)
    }
}
