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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.data.impl.UserRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.subscribeWith
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.model.user.Relationship
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import javax.inject.Inject
import kotlin.math.min

class UserActivityViewStates @Inject constructor(
    tweetingUser: TweetingUser,
    actions: UserActivityActions,
    userRepository: UserRepository,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
    navigationDelegate: UserActivityNavigationDelegate
) {
    val user: LiveData<User?> = userRepository.getUser(tweetingUser.id)
    val relationship: LiveData<Relationship?> = user.switchMap {
        when (it) {
            null -> MutableLiveData()
            else -> relationshipRepository.findRelationship(it.id)
        }
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

    init {
        with(navigationDelegate) {
            subscribeWith(actions.changeFollowingStatus) {
                relationshipRepository.updateFollowingStatus(it.targetUserId, it.wantsFollow)
            }
            subscribeWith(actions.changeBlockingStatus) {
                relationshipRepository.updateBlockingStatus(it.targetUserId, it.wantsBlock)
            }
            subscribeWith(actions.changeRetweetBlockingStatus) {
                relationshipRepository.updateWantRetweetStatus(it.targetUserId, it.wantsRetweet)
            }
            subscribeWith(actions.changeMutingStatus) {
                relationshipRepository.updateMutingStatus(it.targetUserId, it.wantsMute)
            }
            subscribeWith(actions.reportSpam) {
                relationshipRepository.reportSpam(it.targetUserId)
            }
            subscribeWith(actions.rollbackViewState) { dispatchBack() }
        }
    }
}
