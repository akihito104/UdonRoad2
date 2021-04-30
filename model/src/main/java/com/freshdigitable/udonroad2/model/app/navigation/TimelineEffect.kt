/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.model.app.navigation

import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.user.TweetUserItem

sealed class TimelineEffect : AppEffect {
    data class ToTopOfList(val needsSkip: Boolean) : TimelineEffect()

    sealed class Navigate : AppEffect.Navigation, TimelineEffect() {
        data class Timeline(
            val owner: ListOwner<*>,
            override val type: AppEffect.Navigation.Type = AppEffect.Navigation.Type.NAVIGATE,
        ) : Navigate()

        data class Detail(
            val id: TweetId,
        ) : Navigate()

        data class UserInfo(val tweetUserItem: TweetUserItem) : Navigate()

        data class MediaViewer(
            val tweetId: TweetId,
            val index: Int = 0,
        ) : Navigate() {
            companion object
        }
    }
}

suspend fun ListOwnerGenerator.getTimelineEvent(
    queryType: QueryType,
    navType: AppEffect.Navigation.Type = AppEffect.Navigation.Type.NAVIGATE,
): TimelineEffect.Navigate.Timeline = TimelineEffect.Navigate.Timeline(generate(queryType), navType)
