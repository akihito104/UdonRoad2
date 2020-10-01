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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.app.AppExecutor
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlin.math.min

class UserActivityViewStates @Inject constructor(
    tweetingUser: TweetingUser,
    actions: UserActivityActions,
    userRepository: UserRepository,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
    private val navigationDelegate: UserActivityNavigationDelegate,
    executor: AppExecutor,
) {
    val user: LiveData<User?> = userRepository.getUser(tweetingUser.id)
    val relationship: AppViewState<Relationship?> = user.switchMap {
        when (it) {
            null -> MutableLiveData()
            else -> relationshipRepository.findRelationship(it.id)
        }
    }
    val relationshipMenuItems: AppViewState<Set<RelationshipMenu>> = relationship.map {
        RelationshipMenu.availableItems(it)
    }

    // TODO: save to state handle
    val pages: Map<UserPage, ListOwner<*>> = UserPage.values()
        .map { it to ownerGenerator.create(it.createQuery(tweetingUser)) }
        .toMap()

    private val currentPage: AppViewState<UserPage> = actions.currentPageChanged
        .map { it.page }
        .toViewState()
    val selectedItemId = currentPage.switchMap {
        selectedItemRepository.observe(requireNotNull(pages[it]))
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
    private val feedbackMessage: AppAction<FeedbackMessage> = AppAction.merge(listOf(
        actions.changeFollowingStatus.suspendMap(executor.dispatcher.mainContext) {
            relationshipRepository.updateFollowingStatus(it.targetUserId, it.wantsFollow)
        }.map {
            if (it.event.wantsFollow) {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.FOLLOW_CREATE_SUCCESS
                    else -> RelationshipFeedbackMessage.FOLLOW_CREATE_FAILURE
                }
            } else {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.FOLLOW_DESTROY_SUCCESS
                    else -> RelationshipFeedbackMessage.FOLLOW_DESTROY_FAILURE
                }
            }
        },
        actions.changeBlockingStatus.suspendMap(executor.dispatcher.mainContext) {
            relationshipRepository.updateBlockingStatus(it.targetUserId, it.wantsBlock)
        }.map {
            if (it.event.wantsBlock) {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.BLOCK_CREATE_SUCCESS
                    else -> RelationshipFeedbackMessage.BLOCK_CREATE_FAILURE
                }
            } else {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.BLOCK_DESTROY_SUCCESS
                    else -> RelationshipFeedbackMessage.BLOCK_DESTROY_FAILURE
                }
            }
        },
        actions.changeMutingStatus.suspendMap(executor.dispatcher.mainContext) {
            relationshipRepository.updateMutingStatus(it.targetUserId, it.wantsMute)
        }.map {
            if (it.event.wantsMute) {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.MUTE_CREATE_SUCCESS
                    else -> RelationshipFeedbackMessage.MUTE_CREATE_FAILURE
                }
            } else {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.MUTE_DESTROY_SUCCESS
                    else -> RelationshipFeedbackMessage.MUTE_DESTROY_FAILURE
                }
            }
        },
        actions.changeRetweetBlockingStatus.suspendMap(executor.dispatcher.mainContext) {
            relationshipRepository.updateWantRetweetStatus(it.targetUserId, it.wantsRetweet)
        }.map {
            if (it.event.wantsRetweet) {
                when (it.isSuccess) {
                    it.isSuccess -> RelationshipFeedbackMessage.WANT_RETWEET_CREATE_SUCCESS
                    else -> RelationshipFeedbackMessage.WANT_RETWEET_CREATE_FAILURE
                }
            } else {
                when {
                    it.isSuccess -> RelationshipFeedbackMessage.WANT_RETWEET_DESTROY_SUCCESS
                    else -> RelationshipFeedbackMessage.WANT_RETWEET_DESTROY_FAILURE
                }
            }
        },
        actions.reportSpam.suspendMap(executor.dispatcher.mainContext) {
            relationshipRepository.reportSpam(it.targetUserId)
        }.map {
            when {
                it.isSuccess -> RelationshipFeedbackMessage.REPORT_SPAM_SUCCESS
                else -> RelationshipFeedbackMessage.REPORT_SPAM_FAILURE
            }
        }
    ))

    private val disposables = CompositeDisposable(
        feedbackMessage.subscribe { navigationDelegate.dispatchFeedbackMessage(it) },
        actions.rollbackViewState.subscribe { navigationDelegate.dispatchBack() },
    )

    fun clear() {
        disposables.clear()
        navigationDelegate.clear()
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
    REPORT_SPAM_FAILURE(R.string.msg_report_spam_failed)
    ;
}
