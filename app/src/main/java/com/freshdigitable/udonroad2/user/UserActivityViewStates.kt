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
import com.freshdigitable.udonroad2.data.impl.AppExecutor
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.EventResult
import com.freshdigitable.udonroad2.model.app.navigation.FeedbackMessage
import com.freshdigitable.udonroad2.model.app.navigation.subscribeWith
import com.freshdigitable.udonroad2.model.app.navigation.suspendMap
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlin.math.min

@ExperimentalCoroutinesApi
class UserActivityViewStates @Inject constructor(
    tweetingUser: TweetingUser,
    actions: UserActivityActions,
    userRepository: UserRepository,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
    navigationDelegate: UserActivityNavigationDelegate,
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

    private val feedbackMessage: AppAction<FeedbackMessage> = AppAction.merge(listOf(
        actions.changeFollowingStatus.suspendMap(executor.dispatcher.ioContext) {
            relationshipRepository.updateFollowingStatus(it.targetUserId, it.wantsFollow)
        },
        actions.changeBlockingStatus.suspendMap(executor.dispatcher.ioContext) {
            relationshipRepository.updateBlockingStatus(it.targetUserId, it.wantsBlock)
        },
        actions.changeMutingStatus.suspendMap(executor.dispatcher.ioContext) {
            relationshipRepository.updateMutingStatus(it.targetUserId, it.wantsMute)
        },
        actions.changeRetweetBlockingStatus.suspendMap(executor.dispatcher.ioContext) {
            relationshipRepository.updateWantRetweetStatus(it.targetUserId, it.wantsRetweet)
        },
        actions.reportSpam.suspendMap(executor.dispatcher.ioContext) {
            relationshipRepository.reportSpam(it.targetUserId)
        }
    )).map {
        RelationshipFeedbackMessage.find(it)
    }

    init {
        with(navigationDelegate) {
            subscribeWith(feedbackMessage) { navigationDelegate.dispatchFeedbackMessage(it) }
            subscribeWith(actions.rollbackViewState) { dispatchBack() }
        }
    }
}

@Keep
@Suppress("unused")
internal enum class RelationshipFeedbackMessage(
    override val messageRes: Int,
    private val matcher: (EventResult<*>) -> Boolean,
) : FeedbackMessage {
    FOLLOW_CREATE_SUCCESS(R.string.msg_follow_create_success, {
        (it.event as? UserActivityEvent.Relationships.Following)?.wantsFollow == true
            && it.isSuccess
    }),
    FOLLOW_CREATE_FAILURE(R.string.msg_follow_create_failure, {
        (it.event as? UserActivityEvent.Relationships.Following)?.wantsFollow == true
            && it.isFailure
    }),
    FOLLOW_DESTROY_SUCCESS(R.string.msg_follow_destroy_success, {
        (it.event as? UserActivityEvent.Relationships.Following)?.wantsFollow == false
            && it.isSuccess
    }),
    FOLLOW_DESTROY_FAILURE(R.string.msg_follow_destroy_failure, {
        (it.event as? UserActivityEvent.Relationships.Following)?.wantsFollow == false
            && it.isFailure
    }),

    MUTE_CREATE_SUCCESS(R.string.msg_mute_create_success, {
        (it.event as? UserActivityEvent.Relationships.Muting)?.wantsMute == true
            && it.isSuccess
    }),
    MUTE_CREATE_FAILURE(R.string.msg_mute_create_failure, {
        (it.event as? UserActivityEvent.Relationships.Muting)?.wantsMute == true
            && it.isFailure
    }),
    MUTE_DESTROY_SUCCESS(R.string.msg_mute_destroy_success, {
        (it.event as? UserActivityEvent.Relationships.Muting)?.wantsMute == false
            && it.isSuccess
    }),
    MUTE_DESTROY_FAILURE(R.string.msg_mute_destroy_failure, {
        (it.event as? UserActivityEvent.Relationships.Muting)?.wantsMute == false
            && it.isFailure
    }),

    BLOCK_CREATE_SUCCESS(R.string.msg_block_create_success, {
        (it.event as? UserActivityEvent.Relationships.Blocking)?.wantsBlock == true
            && it.isSuccess
    }),
    BLOCK_CREATE_FAILURE(R.string.msg_block_create_failure, {
        (it.event as? UserActivityEvent.Relationships.Blocking)?.wantsBlock == true
            && it.isFailure
    }),
    BLOCK_DESTROY_SUCCESS(R.string.msg_block_destroy_success, {
        (it.event as? UserActivityEvent.Relationships.Blocking)?.wantsBlock == false
            && it.isSuccess
    }),
    BLOCK_DESTROY_FAILURE(R.string.msg_block_destroy_failure, {
        (it.event as? UserActivityEvent.Relationships.Blocking)?.wantsBlock == false
            && it.isFailure
    }),

    WANT_RETWEET_CREATE_SUCCESS(R.string.msg_want_retweet_create_success, {
        (it.event as? UserActivityEvent.Relationships.WantsRetweet)?.wantsRetweet == true
            && it.isSuccess
    }),
    WANT_RETWEET_CREATE_FAILURE(R.string.msg_want_retweet_create_failure, {
        (it.event as? UserActivityEvent.Relationships.WantsRetweet)?.wantsRetweet == true
            && it.isFailure
    }),
    WANT_RETWEET_DESTROY_SUCCESS(R.string.msg_want_retweet_destroy_success, {
        (it.event as? UserActivityEvent.Relationships.WantsRetweet)?.wantsRetweet == false
            && it.isSuccess
    }),
    WANT_RETWEET_DESTROY_FAILURE(R.string.msg_want_retweet_destroy_failure, {
        (it.event as? UserActivityEvent.Relationships.WantsRetweet)?.wantsRetweet == false
            && it.isFailure
    }),

    REPORT_SPAM_SUCCESS(R.string.msg_report_spam_success, {
        it.event is UserActivityEvent.Relationships.ReportSpam && it.isSuccess
    }),
    REPORT_SPAM_FAILURE(R.string.msg_report_spam_failed, {
        it.event is UserActivityEvent.Relationships.ReportSpam && it.isFailure
    })
    ;

    companion object {
        fun find(result: EventResult<*>): FeedbackMessage? = values().find { it.matcher(result) }
    }
}
