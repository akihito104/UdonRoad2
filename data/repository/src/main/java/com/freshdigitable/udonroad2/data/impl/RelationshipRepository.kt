package com.freshdigitable.udonroad2.data.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.restclient.FriendshipRestClient
import com.freshdigitable.udonroad2.model.user.Relationship
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

    fun updateFollowingStatus(targetUserId: UserId, isFollowing: Boolean) {
        executor.launchIO {
            val user = if (isFollowing) {
                restClient.createFriendship(targetUserId)
            } else {
                restClient.destroyFriendship(targetUserId)
            }
            dao.updateFollowingStatus(user.id, isFollowing)
        }
    }

    fun updateMutingStatus(targetUserId: UserId, isMuting: Boolean) {
        executor.launchIO {
            val user = when {
                isMuting -> restClient.createMute(targetUserId)
                else -> restClient.destroyMute(targetUserId)
            }
            dao.updateMutingStatus(user.id, isMuting)
        }
    }

    fun updateBlockingStatus(targetUserId: UserId, isBlocking: Boolean) {
        executor.launchIO {
            val user = when {
                isBlocking -> restClient.createBlock(targetUserId)
                else -> restClient.destroyBlock(targetUserId)
            }
            dao.updateBlockingStatus(user.id, isBlocking)
        }
    }

    fun updateWantRetweetStatus(userId: UserId, wantRetweets: Boolean) {
        executor.launchIO {
            val currentRelation = restClient.fetchFriendship(userId)
            val updated = restClient.updateFriendship(
                currentRelation.userId,
                currentRelation.notificationsEnabled,
                wantRetweets
            )
            dao.updateWantRetweetsStatus(updated.userId, updated.wantRetweets)
        }
    }

    fun reportSpam(userId: UserId) {
        executor.launchIO {
            val user = restClient.reportSpam(userId)
            dao.updateBlockingStatus(user.id, false)
        }
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
