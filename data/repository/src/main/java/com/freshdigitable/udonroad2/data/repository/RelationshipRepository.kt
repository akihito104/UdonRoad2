package com.freshdigitable.udonroad2.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.freshdigitable.udonroad2.data.db.DaoModule
import com.freshdigitable.udonroad2.data.db.dao.RelationshipDao
import com.freshdigitable.udonroad2.data.restclient.FriendshipRestClient
import com.freshdigitable.udonroad2.data.restclient.TwitterModule
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
            withContext(Dispatchers.IO) {
                dao.addRelationship(f)
            }
        }
    }
}

@Module(includes = [DaoModule::class, TwitterModule::class])
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
