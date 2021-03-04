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

import android.view.MenuItem
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.app.navigation.AppAction
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.user.UserActivityEvent.Relationships
import javax.inject.Inject

class UserActions @Inject constructor(
    private val tweetUserItem: TweetUserItem,
    private val eventDispatcher: EventDispatcher
) : UserViewEventListener {

    override fun onAppBarScrolled(rate: Float) {
        eventDispatcher.postEvent(UserActivityEvent.AppbarScrolled(rate))
    }

    override fun onCurrentPageChanged(index: Int) {
        eventDispatcher.postEvent(UserActivityEvent.PageChanged(UserPage.values()[index]))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return eventDispatcher.postRelationshipEvent(tweetUserItem.id, item)
    }

    override fun onBackPressed(): Boolean {
        TODO("Not yet implemented")
    }

    internal val currentPageChanged: AppAction<UserActivityEvent.PageChanged> =
        eventDispatcher.toAction()
    internal val scrollAppbar: AppAction<UserActivityEvent.AppbarScrolled> =
        eventDispatcher.toAction()
    internal val changeRelationships: AppAction<Relationships> = eventDispatcher.toAction()
}

private fun EventDispatcher.postRelationshipEvent(userId: UserId, item: MenuItem): Boolean {
    val menuItem = RelationshipMenu.findById(item.itemId) ?: return false
    postEvent(menuItem.event(userId))
    return true
}
