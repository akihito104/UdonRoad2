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

package com.freshdigitable.udonroad2.test_common.jvm

import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.app.AppTwitterException
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.MockVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import org.junit.rules.TestRule

class TweetRepositoryRule(
    private val mockVerified: MockVerified<TweetRepository> = MockVerified.create()
) : TestRule by mockVerified {
    val mock: TweetRepository = mockVerified.mock

    fun setupShowTweet(id: TweetId, response: Flow<TweetListItem?>) {
        mockVerified.setupResponseWithVerify({ mock.getDetailTweetItemSource(id) }, response)
    }

    fun setupFindTweetItem(id: TweetId, response: TweetListItem?) {
        mockVerified.coSetupResponseWithVerify({ mock.findDetailTweetItem(id) }, response)
    }

    fun setupFindTweetItem(id: TweetId, throwable: Throwable) {
        mockVerified.coSetupThrowWithVerify({ mock.findDetailTweetItem(id) }, throwable)
    }

    fun setupPostLikeForSuccess(tweetId: TweetId, liked: TweetEntity = mockk()) {
        mockVerified.coSetupResponseWithVerify({ mock.updateLike(tweetId, true) }, liked)
    }

    fun setupPostLikeForFailure(tweetId: TweetId, exceptionType: AppTwitterException.ErrorType) {
        val exception = createException(exceptionType)
        mockVerified.coSetupThrowWithVerify({ mock.updateLike(tweetId, true) }, exception)
    }

    fun setupPostRetweetForSuccess(tweetId: TweetId, retweeted: TweetEntity = mockk()) {
        mockVerified.coSetupResponseWithVerify({ mock.updateRetweet(tweetId, true) }, retweeted)
    }

    fun setupPostRetweetForFailure(tweetId: TweetId, exceptionType: AppTwitterException.ErrorType) {
        val exception = createException(exceptionType)
        mockVerified.coSetupThrowWithVerify({ mock.updateRetweet(tweetId, true) }, exception)
    }

    private fun createException(exceptionType: AppTwitterException.ErrorType): AppTwitterException {
        return AppTwitterException(exceptionType.statusCode, exceptionType.errorCode)
    }
}
