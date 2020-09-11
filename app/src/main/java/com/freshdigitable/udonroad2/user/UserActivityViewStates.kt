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

import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.freshdigitable.udonroad2.data.impl.RelationshipRepository
import com.freshdigitable.udonroad2.data.impl.SelectedItemRepository
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.app.navigation.AppViewState
import com.freshdigitable.udonroad2.model.app.navigation.subscribeWith
import com.freshdigitable.udonroad2.model.app.navigation.toViewState
import com.freshdigitable.udonroad2.model.user.TweetingUser
import javax.inject.Inject

class UserActivityViewStates @Inject constructor(
    tweetingUser: TweetingUser,
    actions: UserActivityActions,
    relationshipRepository: RelationshipRepository,
    selectedItemRepository: SelectedItemRepository,
    ownerGenerator: ListOwnerGenerator,
    navigationDelegate: UserActivityNavigation
) {
    // TODO: save to state handle
    val pages: Map<UserPage, ListOwner<*>> = UserPage.values()
        .map { it to ownerGenerator.create(it.createQuery(tweetingUser)) }
        .toMap()

    private val currentPage: AppViewState<UserPage> = actions.currentPageChanged
        .map { it.page }
        .toViewState()
    val fabVisible: AppViewState<Boolean> = currentPage
        .switchMap { selectedItemRepository.observe(requireNotNull(pages[it])) }
        .map { it != null }

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
        }
    }
}
