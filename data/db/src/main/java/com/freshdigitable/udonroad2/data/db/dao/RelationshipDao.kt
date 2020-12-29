package com.freshdigitable.udonroad2.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserId
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

    fun getRelationshipSource(targetUserId: UserId): Flow<Relationship?> {
        return findRelationshipByTargetUserId(targetUserId).distinctUntilChanged()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun insertRelationship(relationship: RelationshipEntity)

    @Query("UPDATE relationship SET following = :isFollowing WHERE user_id = :userId")
    abstract suspend fun updateFollowingStatus(userId: UserId, isFollowing: Boolean)

    @Query("UPDATE relationship SET muting = :isMuting WHERE user_id = :userId")
    abstract suspend fun updateMutingStatus(userId: UserId, isMuting: Boolean)

    @Transaction
    open suspend fun updateBlockingStatusTransaction(userId: UserId, isBlocking: Boolean) {
        updateBlockingStatus(userId, isBlocking)
        if (isBlocking) {
            updateFollowingStatus(userId, false)
        }
    }

    @Query("UPDATE relationship SET blocking = :isBlocking WHERE user_id = :userId")
    internal abstract suspend fun updateBlockingStatus(userId: UserId, isBlocking: Boolean)

    @Query("UPDATE relationship SET want_retweets = :wantRetweets WHERE user_id = :userId")
    abstract suspend fun updateWantRetweetsStatus(userId: UserId, wantRetweets: Boolean)

    @Query("SELECT * FROM relationship WHERE user_id = :userId")
    internal abstract fun findRelationshipByTargetUserId(
        userId: UserId
    ): Flow<RelationshipEntity?>

    private fun Relationship.toEntity(): RelationshipEntity {
        return RelationshipEntity(
            userId,
            following,
            blocking,
            muting,
            wantRetweets,
            notificationsEnabled
        )
    }
}
