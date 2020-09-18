package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.restclient.AppTwitterException
import com.freshdigitable.udonroad2.data.restclient.FriendshipRestClient
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.model.user.UserId
import dagger.Module
import dagger.Provides
import javax.inject.Inject

class RelationshipRepository @Inject constructor(
    private val dao: RelationshipDao,
    private val restClient: FriendshipRestClient,
    private val executor: AppExecutor
) {
    fun findRelationship(targetUserId: UserId): LiveData<Relationship?> {
        fetchFriendship(targetUserId)
        val res = MediatorLiveData<Relationship?>()
        res.addSource(dao.findRelationship(targetUserId)) { r ->
            when {
                r != null -> res.value = r
                else -> fetchFriendship(targetUserId)
            }
        }
        return res
    }

    private fun fetchFriendship(targetUserId: UserId) {
        executor.launchIO {
            val f = restClient.fetchFriendship(targetUserId)
            dao.addRelationship(f)
        }
    }

    suspend fun updateFollowingStatus(targetUserId: UserId, isFollowing: Boolean): User {
        try {
            val user = when {
                isFollowing -> restClient.createFriendship(targetUserId)
                else -> restClient.destroyFriendship(targetUserId)
            }
            dao.updateFollowingStatus(user.id, isFollowing)
            return user
        } catch (e: AppTwitterException) {
            fetchFriendship(targetUserId)
            throw e
        }
    }

    suspend fun updateMutingStatus(targetUserId: UserId, isMuting: Boolean): User {
        try {
            val user = when {
                isMuting -> restClient.createMute(targetUserId)
                else -> restClient.destroyMute(targetUserId)
            }
            dao.updateMutingStatus(user.id, isMuting)
            return user
        } catch (e: AppTwitterException) {
            fetchFriendship(targetUserId)
            throw e
        }
    }

    suspend fun updateBlockingStatus(targetUserId: UserId, isBlocking: Boolean): User {
        try {
            val user = when {
                isBlocking -> restClient.createBlock(targetUserId)
                else -> restClient.destroyBlock(targetUserId)
            }
            dao.updateBlockingStatusTransaction(user.id, isBlocking)
            return user
        } catch (e: AppTwitterException) {
            fetchFriendship(targetUserId)
            throw e
        }
    }

    suspend fun updateWantRetweetStatus(userId: UserId, wantRetweets: Boolean): Relationship {
        try {
            val currentRelation = restClient.fetchFriendship(userId)
            val updated = restClient.updateFriendship(
                currentRelation.userId,
                currentRelation.notificationsEnabled,
                wantRetweets
            )
            dao.updateWantRetweetsStatus(updated.userId, updated.wantRetweets)
            return updated
        } catch (e: AppTwitterException) {
            fetchFriendship(userId)
            throw e
        }
    }

    suspend fun reportSpam(userId: UserId): User {
        val user = restClient.reportSpam(userId)
        dao.updateBlockingStatusTransaction(user.id, true)
        return user
    }
}

@Module(
    includes = [
        DaoModule::class
    ]
)
object RelationshipRepositoryModule {
    @Provides
    fun provideRelationshipRepository(
        dao: RelationshipDao,
        restClient: FriendshipRestClient,
        executor: AppExecutor
    ): RelationshipRepository {
        return RelationshipRepository(dao, restClient, executor)
    }
}
