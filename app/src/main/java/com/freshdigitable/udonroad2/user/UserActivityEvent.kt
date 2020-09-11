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

import com.freshdigitable.udonroad2.model.app.navigation.AppEvent
import com.freshdigitable.udonroad2.model.user.UserId

sealed class UserActivityEvent : AppEvent {
    data class PageChanged(val page: UserPage) : UserActivityEvent()

    sealed class Relationships : UserActivityEvent() {
        data class Following(
            val wantsFollow: Boolean,
            override val targetUserId: UserId
        ) : Relationships()

        data class Blocking(
            val wantsBlock: Boolean,
            override val targetUserId: UserId
        ) : Relationships()

        data class WantsRetweet(
            val wantsRetweet: Boolean,
            override val targetUserId: UserId
        ) : Relationships()

        data class Muting(
            val wantsMute: Boolean,
            override val targetUserId: UserId
        ) : Relationships()

        data class ReportSpam(
            override val targetUserId: UserId
        ) : Relationships()

        abstract val targetUserId: UserId
    }
}
