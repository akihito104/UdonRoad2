package com.freshdigitable.udonroad2.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.model.Relationship

@Dao
abstract class RelationshipDao {

    suspend fun addRelationship(relationship: Relationship) {
        val r = when (relationship) {
            is RelationshipEntity -> relationship
            else -> relationship.toEntity()
        }
        insertRelationship(r)
    }

    fun findRelationship(targetUserId: Long): LiveData<out Relationship?> {
        return findRelationshipByTargetUserId(targetUserId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun insertRelationship(relationship: RelationshipEntity)

    @Query("UPDATE relationship SET following = :isFollowing WHERE user_id = :userId")
    abstract suspend fun updateFollowingStatus(userId: Long, isFollowing: Boolean)

    @Query("UPDATE relationship SET muting = :isMuting WHERE user_id = :userId")
    abstract suspend fun updateMutingStatus(userId: Long, isMuting: Boolean)

    @Query("UPDATE relationship SET blocking = :isBlocking WHERE user_id = :userId")
    abstract suspend fun updateBlockingStatus(userId: Long, isBlocking: Boolean)

    @Query("UPDATE relationship SET want_retweets = :wantRetweets WHERE user_id = :userId")
    abstract suspend fun updateWantRetweetsStatus(userId: Long, wantRetweets: Boolean)

    @Query("SELECT * FROM relationship WHERE user_id = :userId")
    internal abstract fun findRelationshipByTargetUserId(
        userId: Long
    ): LiveData<RelationshipEntity?>

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
