package com.freshdigitable.udonroad2.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.restclient.FriendshipRestClient
import com.freshdigitable.udonroad2.model.Relationship
import com.freshdigitable.udonroad2.model.RepositoryScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RelationshipRepository @Inject constructor(
    private val dao: RelationshipDao,
    private val restClient: FriendshipRestClient
) {
    fun findRelationship(targetUserId: Long): LiveData<Relationship?> {
        val res = MediatorLiveData<Relationship?>()
        res.addSource(dao.findRelationship(targetUserId)) { r ->
            when {
                r != null -> res.value = r
                else -> fetchFriendship(targetUserId)
            }
        }
        return res
    }

    private fun fetchFriendship(targetUserId: Long) {
        GlobalScope.launch {
            val f = restClient.fetchFriendship(targetUserId)
            dao.addRelationship(f)
        }
    }

    fun updateFollowingStatus(targetUserId: Long, isFollowing: Boolean) {
        GlobalScope.launch {
            val user = if (isFollowing) {
                restClient.createFriendship(targetUserId)
            } else {
                restClient.destroyFriendship(targetUserId)
            }
            withContext(Dispatchers.IO) {
                dao.updateFollowingStatus(user.id, isFollowing)
            }
        }
    }

    fun updateMutingStatus(targetUserId: Long, isMuting: Boolean) {
        GlobalScope.launch {
            val user = if (isMuting) {
                restClient.createMute(targetUserId)
            } else {
                restClient.destroyMute(targetUserId)
            }
            withContext(Dispatchers.IO) {
                dao.updateMutingStatus(user.id, isMuting)
            }
        }
    }

    fun updateBlockingStatus(targetUserId: Long, isBlocking: Boolean) {
        GlobalScope.launch {
            val user = if (isBlocking) {
                restClient.createBlock(targetUserId)
            } else {
                restClient.destroyBlock(targetUserId)
            }
            withContext(Dispatchers.IO) {
                dao.updateBlockingStatus(user.id, isBlocking)
            }
        }
    }

    fun updateWantRetweetStatus(currentRelation: Relationship, wantRetweets: Boolean) {
        GlobalScope.launch {
            val updated = restClient.updateFriendship(
                currentRelation.userId,
                currentRelation.notificationsEnabled,
                wantRetweets
            )
            withContext(Dispatchers.IO) {
                dao.updateWantRetweetsStatus(updated.userId, updated.wantRetweets)
            }
        }
    }

    fun reportSpam(userId: Long) {
        GlobalScope.launch {
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
    @JvmStatic
    @RepositoryScope
    @Provides
    fun provideRelationshipRepository(
        dao: RelationshipDao,
        restClient: FriendshipRestClient
    ): RelationshipRepository {
        return RelationshipRepository(dao, restClient)
    }
}
