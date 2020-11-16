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
import com.freshdigitable.udonroad2.model.tweet.Tweet
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.UserReplyEntity
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.UserId
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
        val originalTweetUser = createTweetingUser(UserId(200), "user200")
        val bodyTweetUser = createTweetingUser(UserId(300), "user300")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun create() = Param.values()
    }

    enum class Param(
        val tweetingUser: TweetingUser,
        val isTargetRetweet: Boolean = false,
        val targetBodyTweetId: TweetId = selectedTweetId,
        val targetBodyTweetingUser: TweetingUser = tweetingUser,
        val replyEntities: List<UserReplyEntity> = emptyList(),
        val expectedText: String,
    ) {
        SimpleTweet(
            tweetingUser = originalTweetUser,
            expectedText = "@user200 "
        ),
        TweetOfMine(
            tweetingUser = createTweetingUser(authenticatedUserId, authenticatedUserScreenName),
            expectedText = ""
        ),
        SimpleTweetWithReplyEntity(
            tweetingUser = originalTweetUser,
            replyEntities = listOf(createReplyEntity(UserId(400), "user400")),
            expectedText = "@user400 @user200 "
        ),
        SimpleRetweet(
            tweetingUser = originalTweetUser,
            isTargetRetweet = true,
            targetBodyTweetId = TweetId(3000),
            targetBodyTweetingUser = bodyTweetUser,
            expectedText = "@user200 @user300 "
        ),
        RetweetWithReplyEntity(
            tweetingUser = originalTweetUser,
            isTargetRetweet = true,
            targetBodyTweetId = TweetId(3000),
            targetBodyTweetingUser = bodyTweetUser,
            replyEntities = listOf(createReplyEntity(UserId(400), "user400")),
            expectedText = "@user400 @user200 @user300 "
        ),
        ;

        override fun toString(): String = "$name: $expectedText"
    }

    @Before
    fun setup() = with(param) {
        val tweet: TweetListItem = mockk<TweetListItem>().apply {
            every { originalId } returns selectedTweetId
            every { originalUser } returns tweetingUser
            every { isRetweet } returns isTargetRetweet
            every { body } returns createTweet(targetBodyTweetId, targetBodyTweetingUser)
        }
        oAuthTokenRepositoryRule.setupCurrentUserId(authenticatedUserId.value)
        tweetRepositoryRule.coSetupResponseWithVerify(
            target = { tweetRepositoryRule.mock.findTweetListItem(selectedTweetId) },
            res = tweet
        )
        replyRepositoryRule.coSetupResponseWithVerify(
            target = { replyRepositoryRule.mock.findEntitiesByTweetId(selectedTweetId) },
            res = replyEntities
        )
        oAuthTokenRepositoryRule.mock.login(authenticatedUserId)
    }

    @Test
    fun testInvoke() = runBlocking {
        val sut = CreateReplyTextUseCase(
            tweetRepositoryRule.mock,
            replyRepositoryRule.mock,
            oAuthTokenRepositoryRule.mock
        )

        // exercise
        val actual = sut(selectedTweetId)

        // verify
        assertThat(actual).isEqualTo(param.expectedText)
    }
}

fun createTweetingUser(targetUserId: UserId, targetUserScreenName: String): TweetingUser {
    return mockk<TweetingUser>().apply {
        every { id } returns targetUserId
        every { screenName } returns targetUserScreenName
    }
}

private fun createTweet(targetTweetId: TweetId, tweetingUser: TweetingUser): Tweet {
    return mockk<Tweet>().apply {
        every { id } returns targetTweetId
        every { user } returns tweetingUser
    }
}

private fun createReplyEntity(userId: UserId, screenName: String): UserReplyEntity {
    return mockk<UserReplyEntity>().apply {
        every { this@apply.userId } returns userId
        every { this@apply.screenName } returns screenName
    }
}
