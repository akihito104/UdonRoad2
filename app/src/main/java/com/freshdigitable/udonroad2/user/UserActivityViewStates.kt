/*
 * Copyright (c) 2020. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2.user

import androidx.annotation.Keep
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.mainContext
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.math.min

class UserActivityViewStates @Inject constructor(
    tweetUserItem: TweetUserItem,
    actions: UserActivityActions,
    userRepository: UserRepository,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
    executor: AppExecutor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _user: Flow<Result<UserEntity>> = userRepository.getUserFlow(tweetUserItem.id)
        .map { user ->
            when (user) {
                null -> {
                    kotlin.runCatching { userRepository.getUser(tweetUserItem.id) }
                        .onFailure { if (it is RuntimeException) throw it }
                }
                else -> Result.success(user)
            }
        }
        .shareIn(scope, SharingStarted.Eagerly)
    internal val user: Flow<UserEntity> = _user.filter { it.isSuccess }
        .map { it.getOrThrow() }
        .shareIn(scope, SharingStarted.Lazily)

    private val _relationship: Flow<Result<Relationship?>> = user.map { it.id }
        .filter { it == tweetUserItem.id }
        .distinctUntilChanged()
        .map {
            kotlin.runCatching {
                relationshipRepository.findRelationship(tweetUserItem.id)
            }.onFailure { if (it is RuntimeException) throw it }
        }.shareIn(scope, SharingStarted.Eagerly)
    internal val relationship: Flow<Relationship?> = merge(
        _relationship.filter { it.isSuccess }.map { it.getOrThrow() },
        _relationship.filter { it.isSuccess }.flatMapLatest {
            relationshipRepository.getRelationshipSource(tweetUserItem.id)
        }
    )
        .shareIn(scope, SharingStarted.Lazily)
    internal val relationshipMenuItems: Flow<Set<RelationshipMenu>> = relationship.map {
        RelationshipMenu.availableItems(it)
    }

    // TODO: save to state handle
    val pages: StateFlow<Map<UserPage, ListOwner<*>>> = flowOf(UserPage.values()).map {
        it.map { p -> p to ownerGenerator.generate(p.createQuery(tweetUserItem)) }.toMap()
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val selectedItemId = combineTransform(
        pages,
        actions.currentPageChanged.asFlow()
            .map { it.page }
            .stateIn(scope, SharingStarted.Eagerly, UserPage.TWEET)
    ) { p, current ->
        if (p.isNotEmpty()) {
            emit(p[current])
        }
    }.asLiveData(executor.mainContext).switchMap {
        selectedItemRepository.observe(requireNotNull(it))
    }
    val fabVisible: AppViewState<Boolean> = selectedItemId
        .map { it != null }

    val titleAlpha: LiveData<Float> = actions.scrollAppbar.map { r ->
        if (r.scrollRate >= 0.9f) {
            min((r.scrollRate - 0.9f) * 10, 1f)
        } else {
            0f
        }
    }
        .distinctUntilChanged()
        .toViewState()

    @ExperimentalCoroutinesApi
    internal val feedbackMessage: Flow<FeedbackMessage> = merge(
        _user.filter { it.isFailure }.map { UserResourceFeedbackMessage.FAILED_FETCH },
        _relationship.filter { it.isFailure }.map { UserResourceFeedbackMessage.FAILED_FETCH },
        actions.changeRelationships.asFlow().mapLatest { event ->
            kotlin.runCatching { relationshipRepository.updateStatus(event) }.fold(
                onSuccess = { findSuccessFeedbackMessage(event) },
                onFailure = { findFailureFeedbackMessage(event) }
            )
        }
    )

    fun clear() {
        scope.cancel()
    }

    companion object {
        private suspend fun RelationshipRepository.updateStatus(
            event: UserActivityEvent.Relationships
        ): Any = when (event) {
            is UserActivityEvent.Relationships.Following -> updateFollowingStatus(
                event.targetUserId, event.wantsFollow
            )
            is UserActivityEvent.Relationships.Blocking -> updateBlockingStatus(
                event.targetUserId, event.wantsBlock
            )
            is UserActivityEvent.Relationships.WantsRetweet -> updateWantRetweetStatus(
                event.targetUserId, event.wantsRetweet
            )
            is UserActivityEvent.Relationships.Muting -> updateMutingStatus(
                event.targetUserId, event.wantsMute
            )
            is UserActivityEvent.Relationships.ReportSpam -> reportSpam(event.targetUserId)
        }

        private fun findSuccessFeedbackMessage(
            event: UserActivityEvent.Relationships
        ): RelationshipFeedbackMessage = when (event) {
            is UserActivityEvent.Relationships.Following -> {
                if (event.wantsFollow) RelationshipFeedbackMessage.FOLLOW_CREATE_SUCCESS
                else RelationshipFeedbackMessage.FOLLOW_DESTROY_SUCCESS
            }
            is UserActivityEvent.Relationships.Blocking -> {
                if (event.wantsBlock) RelationshipFeedbackMessage.BLOCK_CREATE_SUCCESS
                else RelationshipFeedbackMessage.BLOCK_DESTROY_SUCCESS
            }
            is UserActivityEvent.Relationships.WantsRetweet -> {
                if (event.wantsRetweet) RelationshipFeedbackMessage.WANT_RETWEET_CREATE_SUCCESS
                else RelationshipFeedbackMessage.WANT_RETWEET_DESTROY_SUCCESS
            }
            is UserActivityEvent.Relationships.Muting -> {
                if (event.wantsMute) RelationshipFeedbackMessage.MUTE_CREATE_SUCCESS
                else RelationshipFeedbackMessage.MUTE_DESTROY_SUCCESS
            }
            is UserActivityEvent.Relationships.ReportSpam ->
                RelationshipFeedbackMessage.REPORT_SPAM_SUCCESS
        }

        private fun findFailureFeedbackMessage(
            event: UserActivityEvent.Relationships
        ): RelationshipFeedbackMessage = when (event) {
            is UserActivityEvent.Relationships.Following -> {
                if (event.wantsFollow) RelationshipFeedbackMessage.FOLLOW_CREATE_FAILURE
                else RelationshipFeedbackMessage.FOLLOW_DESTROY_FAILURE
            }
            is UserActivityEvent.Relationships.Blocking -> {
                if (event.wantsBlock) RelationshipFeedbackMessage.BLOCK_CREATE_FAILURE
                else RelationshipFeedbackMessage.BLOCK_DESTROY_FAILURE
            }
            is UserActivityEvent.Relationships.WantsRetweet -> {
                if (event.wantsRetweet) RelationshipFeedbackMessage.WANT_RETWEET_CREATE_FAILURE
                else RelationshipFeedbackMessage.WANT_RETWEET_DESTROY_FAILURE
            }
            is UserActivityEvent.Relationships.Muting -> {
                if (event.wantsMute) RelationshipFeedbackMessage.MUTE_CREATE_FAILURE
                else RelationshipFeedbackMessage.MUTE_DESTROY_FAILURE
            }
            is UserActivityEvent.Relationships.ReportSpam ->
                RelationshipFeedbackMessage.REPORT_SPAM_FAILURE
        }
    }
}

@Keep
@Suppress("unused")
internal enum class RelationshipFeedbackMessage(
    override val messageRes: Int,
) : FeedbackMessage {
    FOLLOW_CREATE_SUCCESS(R.string.msg_follow_create_success),
    FOLLOW_CREATE_FAILURE(R.string.msg_follow_create_failure),
    FOLLOW_DESTROY_SUCCESS(R.string.msg_follow_destroy_success),
    FOLLOW_DESTROY_FAILURE(R.string.msg_follow_destroy_failure),

    MUTE_CREATE_SUCCESS(R.string.msg_mute_create_success),
    MUTE_CREATE_FAILURE(R.string.msg_mute_create_failure),
    MUTE_DESTROY_SUCCESS(R.string.msg_mute_destroy_success),
    MUTE_DESTROY_FAILURE(R.string.msg_mute_destroy_failure),

    BLOCK_CREATE_SUCCESS(R.string.msg_block_create_success),
    BLOCK_CREATE_FAILURE(R.string.msg_block_create_failure),
    BLOCK_DESTROY_SUCCESS(R.string.msg_block_destroy_success),
    BLOCK_DESTROY_FAILURE(R.string.msg_block_destroy_failure),

    WANT_RETWEET_CREATE_SUCCESS(R.string.msg_want_retweet_create_success),
    WANT_RETWEET_CREATE_FAILURE(R.string.msg_want_retweet_create_failure),
    WANT_RETWEET_DESTROY_SUCCESS(R.string.msg_want_retweet_destroy_success),
    WANT_RETWEET_DESTROY_FAILURE(R.string.msg_want_retweet_destroy_failure),

    REPORT_SPAM_SUCCESS(R.string.msg_report_spam_success),
    REPORT_SPAM_FAILURE(R.string.msg_report_spam_failed),

    FETCH_FAILED(R.string.msg_fetch_relationship_failed),
    ;
}

internal enum class UserResourceFeedbackMessage(
    override val messageRes: Int
) : FeedbackMessage {
    FAILED_FETCH(R.string.msg_user_fetch_failure)
}
