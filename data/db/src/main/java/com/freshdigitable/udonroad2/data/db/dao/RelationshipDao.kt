package com.freshdigitable.udonroad2.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.Relationship
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Dao
abstract class RelationshipDao {

    suspend fun addRelationship(relationship: Relationship) {
        val r = when (relationship) {
            is RelationshipEntity -> relationship
            else -> relationship.toEntity()
        }
        insertRelationship(r)
    }

    fun getRelationshipSource(targetUserId: UserId, sourceUserId: UserId): Flow<Relationship?> {
        return findRelationshipByTargetUserId(targetUserId, sourceUserId).distinctUntilChanged()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun insertRelationship(relationship: RelationshipEntity)

    @Query(
        """UPDATE relationship SET following = :isFollowing 
        WHERE user_id = :targetUserId AND source_user_id = :sourceUserId"""
    )
    abstract suspend fun updateFollowingStatus(
        targetUserId: UserId,
        sourceUserId: UserId,
        isFollowing: Boolean
    )

    @Query(
        """UPDATE relationship SET muting = :isMuting 
        WHERE user_id = :userId AND source_user_id = :sourceUserId"""
    )
    abstract suspend fun updateMutingStatus(userId: UserId, sourceUserId: UserId, isMuting: Boolean)

    @Transaction
    open suspend fun updateBlockingStatusTransaction(
        userId: UserId,
        sourceUserId: UserId,
        isBlocking: Boolean
    ) {
        updateBlockingStatus(userId, sourceUserId, isBlocking)
        if (isBlocking) {
            updateFollowingStatus(userId, sourceUserId, false)
        }
    }

    @Query(
        """UPDATE relationship SET blocking = :isBlocking 
        WHERE user_id = :userId AND source_user_id = :sourceUserId"""
    )
    internal abstract suspend fun updateBlockingStatus(
        userId: UserId,
        sourceUserId: UserId,
        isBlocking: Boolean
    )

    @Query(
        """UPDATE relationship SET want_retweets = :wantRetweets 
        WHERE user_id = :userId AND source_user_id = :sourceUserId"""
    )
    abstract suspend fun updateWantRetweetsStatus(
        userId: UserId,
        sourceUserId: UserId,
        wantRetweets: Boolean
    )

    @Query("SELECT * FROM relationship WHERE user_id = :userId AND source_user_id = :sourceUserId")
    internal abstract fun findRelationshipByTargetUserId(
        userId: UserId,
        sourceUserId: UserId,
    ): Flow<RelationshipEntity?>

    private fun Relationship.toEntity(): RelationshipEntity {
        return RelationshipEntity(
            targetUserId,
            following,
            blocking,
            muting,
            wantRetweets,
            notificationsEnabled,
            sourceUserId,
        )
    }
}
