package com.freshdigitable.udonroad2.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.withTransaction
import com.freshdigitable.udonroad2.data.AppSettingDataSource
import com.freshdigitable.udonroad2.data.RelationDataSource
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.entity.RelationshipEntity
import com.freshdigitable.udonroad2.data.local.requireCurrentUserId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@Dao
internal abstract class RelationshipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun insertRelationship(relationship: RelationshipEntity)

    @Query(
        """UPDATE relationship SET following = :isFollowing 
        WHERE user_id = :targetUserId AND source_user_id = :sourceUserId"""
    )
    internal abstract suspend fun updateFollowingStatus(
        targetUserId: UserId,
        sourceUserId: UserId,
        isFollowing: Boolean
    )

    @Query(
        """UPDATE relationship SET muting = :isMuting 
        WHERE user_id = :userId AND source_user_id = :sourceUserId"""
    )
    internal abstract suspend fun updateMutingStatus(
        userId: UserId,
        sourceUserId: UserId,
        isMuting: Boolean
    )

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
    internal abstract suspend fun updateWantRetweetsStatus(
        userId: UserId,
        sourceUserId: UserId,
        wantRetweets: Boolean
    )

    @Query(QUERY_FIND_RELATIONSHIP)
    internal abstract fun getRelationshipSource(
        userId: UserId,
        sourceUserId: UserId,
    ): Flow<RelationshipEntity?>

    @Query(QUERY_FIND_RELATIONSHIP)
    internal abstract fun findRelationship(
        userId: UserId,
        sourceUserId: UserId,
    ): RelationshipEntity?

    internal companion object {
        private const val QUERY_FIND_RELATIONSHIP =
            "SELECT * FROM relationship WHERE user_id = :userId AND source_user_id = :sourceUserId"
    }
}

class RelationshipLocalDataSource @Inject constructor(
    private val db: AppDatabase,
    private val appSetting: AppSettingDataSource.Local,
) : RelationDataSource.Local {
    private val dao: RelationshipDao = db.relationshipDao()
    private val userDao: UserDao = db.userDao()

    override suspend fun updateRelationship(relationship: Relationship) {
        val r = when (relationship) {
            is RelationshipEntity -> relationship
            else -> relationship.toEntity()
        }
        dao.insertRelationship(r)
    }

    override fun getRelationshipSource(targetUserId: UserId): Flow<Relationship?> =
        dao.getRelationshipSource(targetUserId, appSetting.requireCurrentUserId())
            .distinctUntilChanged()

    override suspend fun findRelationship(targetUserId: UserId): Relationship? =
        dao.findRelationship(targetUserId, appSetting.requireCurrentUserId())

    override suspend fun updateFollowingStatus(
        targetUserId: UserId,
        isFollowing: Boolean
    ): UserEntity {
        dao.updateFollowingStatus(targetUserId, appSetting.requireCurrentUserId(), isFollowing)
        return requireNotNull(userDao.getUser(targetUserId))
    }

    override suspend fun updateMutingStatus(targetUserId: UserId, isMuting: Boolean): UserEntity {
        dao.updateMutingStatus(targetUserId, appSetting.requireCurrentUserId(), isMuting)
        return requireNotNull(userDao.getUser(targetUserId))
    }

    override suspend fun updateBlockingStatus(
        targetUserId: UserId,
        isBlocking: Boolean
    ): UserEntity {
        val sourceUserId = appSetting.requireCurrentUserId()
        updateBlockingStatusTransaction(targetUserId, sourceUserId, isBlocking)
        return requireNotNull(userDao.getUser(targetUserId))
    }

    override suspend fun addSpam(userId: UserId): UserEntity {
        updateBlockingStatusTransaction(userId, appSetting.requireCurrentUserId(), true)
        return requireNotNull(userDao.getUser(userId))
    }

    override suspend fun updateWantRetweetStatus(
        targetUserId: UserId,
        wantRetweets: Boolean
    ): Relationship {
        dao.updateWantRetweetsStatus(targetUserId, appSetting.requireCurrentUserId(), wantRetweets)
        return requireNotNull(dao.findRelationship(targetUserId, appSetting.requireCurrentUserId()))
    }

    private suspend fun updateBlockingStatusTransaction(
        targetUserId: UserId,
        sourceUserId: UserId,
        isBlocking: Boolean
    ) {
        db.withTransaction {
            dao.updateBlockingStatus(targetUserId, sourceUserId, isBlocking)
            if (isBlocking) {
                dao.updateFollowingStatus(targetUserId, sourceUserId, false)
            }
        }
    }
}

private fun Relationship.toEntity(): RelationshipEntity = RelationshipEntity(
    targetUserId,
    following,
    blocking,
    muting,
    wantRetweets,
    notificationsEnabled,
    sourceUserId,
)
