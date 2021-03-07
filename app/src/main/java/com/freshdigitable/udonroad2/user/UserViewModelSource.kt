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
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.UserDataSource
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.SelectedItemId
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.onEvent
import com.freshdigitable.udonroad2.model.app.stateSourceBuilder
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.math.min

class UserViewModelSource @Inject constructor(
    tweetUserItem: TweetUserItem,
    actions: UserActions,
    userRepository: UserDataSource,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
) : UserViewEventListener by actions {
    private val feedbackChannel = Channel<FeedbackMessage>()
    internal val state: Flow<State> = stateSourceBuilder(
        initBlock = {
            val pages = UserPage.values().map {
                it to ownerGenerator.generate(it.createQuery(tweetUserItem))
            }.toMap()
            State(pages = pages)
        },
        stateBlock = {
            mapNotNull { it.currentOwner }
                .distinctUntilChanged()
                .flatMapLatest { selectedItemRepository.getSource(it) }
                .onEvent { s, e -> s.copy(selectedItemId = e) }
        },
        actions.currentPageChanged.onEvent { s, e ->
            val listOwner = s.pages[e.page] ?: return@onEvent s.copy(currentPage = e.page)
            s.copy(currentPage = e.page, selectedItemId = selectedItemRepository.find(listOwner))
        },
        actions.scrollAppbar.onEvent { s, r ->
            Timber.tag("UserViewModelSource").d("scrollAppbar: $r")
            val a = if (r.scrollRate >= 0.9f) {
                min((r.scrollRate - 0.9f) * 10, 1f)
            } else {
                0f
            }
            if (s.titleAlpha != a) {
                s.copy(titleAlpha = a)
            } else {
                s
            }
        },
        userRepository.getUserSource(tweetUserItem.id).onEvent { s, u ->
            if (u != null) {
                if (s.relationship != null) {
                    s.copy(user = u)
                } else {
                    val relationshipRes = relationshipRepository.runCatching {
                        findRelationship(tweetUserItem.id)
                    }.onFailure {
                        if (it !is AppTwitterException && it !is IOException) {
                            throw it
                        }
                    }
                    if (relationshipRes.isFailure) {
                        feedbackChannel.send(UserResourceFeedbackMessage.FAILED_FETCH)
                    }
                    s.copy(user = u, relationship = relationshipRes.getOrNull())
                }
            } else {
                val res = userRepository.runCatching {
                    getUser(tweetUserItem.id)
                }.onFailure {
                    if (it !is AppTwitterException && it !is IOException) {
                        throw it
                    }
                }
                if (res.isFailure) {
                    feedbackChannel.send(UserResourceFeedbackMessage.FAILED_FETCH)
                }
                s.copy(user = res.getOrNull())
            }
        },
        relationshipRepository.getRelationshipSource(tweetUserItem.id).onEvent { s, r ->
            s.copy(relationship = r)
        }
    )

    @ExperimentalCoroutinesApi
    internal val feedbackMessage: Flow<FeedbackMessage> = merge(
        feedbackChannel.receiveAsFlow(),
        actions.changeRelationships.mapLatest { event ->
            relationshipRepository.runCatching { updateStatus(event) }.fold(
                onSuccess = { findSuccessFeedbackMessage(event) },
                onFailure = { findFailureFeedbackMessage(event) }
            )
        }
    )

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
            is UserActivityEvent.Relationships.ReportSpam -> addSpam(event.targetUserId)
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

    internal data class State(
        override val user: UserEntity? = null,
        override val relationship: Relationship? = null,
        override val titleAlpha: Float = 0f,
        override val pages: Map<UserPage, ListOwner<*>> = emptyMap(),
        val currentPage: UserPage = UserPage.TWEET,
        override val selectedItemId: SelectedItemId? = null,
    ) : UserViewState {
        override val isShortcutVisible: Boolean
            get() = selectedItemId != null
        val currentOwner: ListOwner<*>?
            get() = pages[currentPage]
        override val relationshipMenuItems: Set<RelationshipMenu>
            get() = RelationshipMenu.availableItems(relationship)
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
