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

package com.freshdigitable.udonroad2.input

import com.freshdigitable.udonroad2.data.impl.AppSettingRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.TweetId
import javax.inject.Inject

class CreateReplyTextUseCase @Inject constructor(
    private val tweetRepository: TweetRepository,
    private val appSettingRepository: AppSettingRepository,
) {
    suspend operator fun invoke(tweetId: TweetId): String {
        val item = checkNotNull(tweetRepository.findDetailTweetItem(tweetId))
        val replyEntity = item.body.replyEntities.map { it.userId to it.screenName }

        val targetUsers = listOfNotNull(
            if (item.isRetweet) item.originalUser.id to item.originalUser.screenName else null,
            item.body.user.id to item.body.user.screenName,
        )

        val currentUser = checkNotNull(appSettingRepository.currentUserId)

        val replied = (replyEntity + targetUsers)
            .filter { (id, _) -> id != currentUser }
            .toSet()
        return if (replied.isEmpty()) {
            ""
        } else
            replied.joinToString(separator = " ", postfix = " ") { (_, name) -> "@$name" }
    }
}

class CreateQuoteTextUseCase @Inject constructor(
    private val tweetRepository: TweetRepository,
) {
    suspend operator fun invoke(tweetId: TweetId): String {
        val item = checkNotNull(tweetRepository.findDetailTweetItem(tweetId)).body
        val screenName = item.user.screenName
        return "https://twitter.com/$screenName/status/${item.id.value}"
    }
}
