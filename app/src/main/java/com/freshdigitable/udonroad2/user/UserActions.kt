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

import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import com.freshdigitable.udonroad2.model.app.navigation.toAction
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import javax.inject.Inject

class UserActions @Inject constructor(
    private val tweetUserItem: TweetUserItem,
    eventDispatcher: EventDispatcher,
) : UserViewEventListener {

    override val scrollAppbar = eventDispatcher.toAction { rate: Float ->
        UserActivityEvent.AppbarScrolled(rate)
    }
    override val changePage = eventDispatcher.toAction { index: Int ->
        UserActivityEvent.PageChanged(UserPage.values()[index])
    }
    override val changeRelationships = eventDispatcher.toAction { menuItem: RelationshipMenu ->
        menuItem.event(tweetUserItem.id)
    }
}
