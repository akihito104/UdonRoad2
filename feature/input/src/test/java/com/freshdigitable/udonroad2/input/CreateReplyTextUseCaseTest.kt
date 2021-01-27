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

import com.freshdigitable.udonroad2.data.ReplyRepository
import com.freshdigitable.udonroad2.data.impl.TweetRepository
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetElement
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.OAuthTokenRepositoryRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CreateReplyTextUseCaseTest(private val param: Param) {
    @get:Rule
    val oAuthTokenRepositoryRule = OAuthTokenRepositoryRule()

    @get:Rule
    val tweetRepositoryRule: MockVerified<TweetRepository> = MockVerified.create()

    @get:Rule
    val replyRepositoryRule: MockVerified<ReplyRepository> = MockVerified.create()

    companion object {
        val authenticatedUserId = UserId(100)
        const val authenticatedUserScreenName = "User1"

        val selectedTweetId: TweetId = TweetId(1000)
        val user200 = tweetingUser(UserId(200), "user200")
        val user300 = tweetingUser(UserId(300), "user300")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun create() = Param.values()
    }

    enum class Param(
        val tweetUserItem: TweetUserItem,
        val isTargetRetweet: Boolean = false,
        val targetBodyTweetId: TweetId = selectedTweetId,
        val targetBodyTweetUserItem: TweetUserItem = tweetUserItem,
        val replyEntities: List<UserReplyEntity> = emptyList(),
        val expectedText: String,
    ) {
        SimpleTweet(
            tweetUserItem = user200,
            expectedText = "@user200 "
        ),
        TweetOfMine(
            tweetUserItem = tweetingUser(authenticatedUserId, authenticatedUserScreenName),
            expectedText = ""
        ),
        SimpleTweetWithReplyEntity(
            tweetUserItem = user200,
            replyEntities = listOf(replyEntity(UserId(400), "user400")),
            expectedText = "@user400 @user200 "
        ),
        RepliedToMe(
            tweetUserItem = user200,
            replyEntities = listOf(replyEntity(authenticatedUserId, authenticatedUserScreenName)),
            expectedText = "@user200 "
        ),
        SimpleRetweet(
            tweetUserItem = user200,
            isTargetRetweet = true,
            targetBodyTweetId = TweetId(3000),
            targetBodyTweetUserItem = user300,
            expectedText = "@user200 @user300 "
        ),
        RetweetWithReplyEntity(
            tweetUserItem = user200,
            isTargetRetweet = true,
            targetBodyTweetId = TweetId(3000),
            targetBodyTweetUserItem = user300,
            replyEntities = listOf(replyEntity(UserId(400), "user400")),
            expectedText = "@user400 @user200 @user300 "
        ),
        ;

        override fun toString(): String = "$name: $expectedText"
    }

    @Before
    fun setup() = with(param) {
        val tweet = tweetItem(
            selectedTweetId,
            tweetUserItem,
            isTargetRetweet,
            tweet(targetBodyTweetId, targetBodyTweetUserItem)
        )
        oAuthTokenRepositoryRule.setupCurrentUserId(authenticatedUserId.value, needLogin = false)
        tweetRepositoryRule.coSetupResponseWithVerify(
            target = { tweetRepositoryRule.mock.findTweetListItem(selectedTweetId) },
            res = tweet
        )
        replyRepositoryRule.coSetupResponseWithVerify(
            target = { replyRepositoryRule.mock.findEntitiesByTweetId(selectedTweetId) },
            res = replyEntities
        )
    }

    @Test
    fun testInvoke() = runBlocking {
        val sut = CreateReplyTextUseCase(
            tweetRepositoryRule.mock,
            replyRepositoryRule.mock,
            oAuthTokenRepositoryRule.appSettingMock
        )

        // exercise
        val actual = sut(selectedTweetId)

        // verify
        assertThat(actual).isEqualTo(param.expectedText)
    }
}

@RunWith(Parameterized::class)
class CreateQuoteTextUseCaseTest(private val param: Param) {
    @get:Rule
    val tweetRepositoryRule: MockVerified<TweetRepository> = MockVerified.create()

    companion object {
        val selectedTweetId: TweetId = TweetId(1000)
        val user200 = tweetingUser(UserId(200), "user200")
        val user300 = tweetingUser(UserId(300), "user300")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun create() = Param.values()
    }

    enum class Param(
        val tweetUserItem: TweetUserItem,
        val isTargetRetweet: Boolean = false,
        val targetBodyTweetId: TweetId = selectedTweetId,
        val targetBodyTweetUserItem: TweetUserItem = tweetUserItem,
        val expectedText: String,
    ) {
        SimpleTweet(
            tweetUserItem = user200,
            expectedText = "https://twitter.com/user200/status/1000"
        ),
        SimpleRetweet(
            tweetUserItem = user200,
            isTargetRetweet = true,
            targetBodyTweetId = TweetId(3000),
            targetBodyTweetUserItem = user300,
            expectedText = "https://twitter.com/user300/status/3000"
        ),
        ;

        override fun toString(): String = "$name: $expectedText"
    }

    @Before
    fun setup() = with(param) {
        val tweet = tweetItem(
            selectedTweetId,
            tweetUserItem,
            isTargetRetweet,
            tweet(targetBodyTweetId, targetBodyTweetUserItem)
        )
        tweetRepositoryRule.coSetupResponseWithVerify(
            target = { tweetRepositoryRule.mock.findTweetListItem(selectedTweetId) },
            res = tweet
        )
    }

    @Test
    fun testInvoke() = runBlocking {
        val sut = CreateQuoteTextUseCase(tweetRepositoryRule.mock)

        // exercise
        val actual = sut(selectedTweetId)

        // verify
        assertThat(actual).isEqualTo(param.expectedText)
    }
}

fun tweetingUser(targetUserId: UserId, targetUserScreenName: String): TweetUserItem {
    return mockk<TweetUserItem>().apply {
        every { id } returns targetUserId
        every { screenName } returns targetUserScreenName
    }
}

private fun tweet(targetTweetId: TweetId, tweetUserItem: TweetUserItem): TweetElement {
    return mockk<TweetElement>().apply {
        every { id } returns targetTweetId
        every { user } returns tweetUserItem
    }
}

private fun tweetItem(
    resOriginalId: TweetId,
    resOriginalUser: TweetUserItem,
    resIsRetweet: Boolean,
    resBody: TweetElement
): TweetListItem {
    return mockk<TweetListItem>().apply {
        every { originalId } returns resOriginalId
        every { originalUser } returns resOriginalUser
        every { isRetweet } returns resIsRetweet
        every { body } returns resBody
    }
}

private fun replyEntity(userId: UserId, screenName: String): UserReplyEntity {
    return mockk<UserReplyEntity>().apply {
        every { this@apply.userId } returns userId
        every { this@apply.screenName } returns screenName
    }
}
