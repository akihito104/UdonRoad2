package com.freshdigitable.udonroad2.data.restclient

import androidx.annotation.WorkerThread
import com.freshdigitable.udonroad2.model.Relationship
import twitter4j.Twitter
import javax.inject.Inject

class FriendshipRestClient @Inject constructor(
    private val twitter: Twitter
) {
    @WorkerThread
    fun fetchFriendship(userId: Long): Relationship {
        val sourceId = twitter.id
        val friendship = twitter.showFriendship(sourceId, userId)
        return friendship.toEntity()
    }
}

private fun twitter4j.Relationship.toEntity(): Relationship {
    return RelationshipEntity(
        userId = this.targetUserId,
        following = this.isSourceFollowingTarget,
        blocking = this.isSourceBlockingTarget,
        muting = this.isSourceMutingTarget,
        wantRetweets = this.isSourceWantRetweets,
        notificationsEnabled = this.isSourceNotificationsEnabled
    )
}

private data class RelationshipEntity(
    override val userId: Long,
    override val following: Boolean,
    override val blocking: Boolean,
    override val muting: Boolean,
    override val wantRetweets: Boolean,
    override val notificationsEnabled: Boolean
) : Relationship
