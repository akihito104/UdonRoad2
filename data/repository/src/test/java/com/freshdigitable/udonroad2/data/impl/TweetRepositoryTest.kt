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

package com.freshdigitable.udonroad2.data.impl

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.plus
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(Enclosed::class)
class TweetRepositoryTest {

    @RunWith(AndroidJUnit4::class)
    class WhenPlainTweetListLoaded {
        companion object {
            private val tweetList = (0..10).map { TweetEntity.create(it) }
            private val retweeted = tweetList[1]
            private val retweetResponse =
                TweetEntity.create(11, retweeted = TweetEntity.create(retweeted.id, true))
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
            setupPostRetweet(retweeted.id, retweetResponse)
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // exercise
            sut.postRetweet(retweeted.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet_passTweetIdOfRetweetResponse(): Unit = rule.runs {
            // setup
            setupPostUnretweet(retweetResponse.id, TweetEntity.create(retweeted.id))
            val res = sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(res.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            setupPostUnretweet(retweetResponse.id, TweetEntity.create(retweeted.id))
            sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(retweeted.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenQuotingTweetListLoaded {
        companion object {
            private val tweetList = (0..10).map {
                TweetEntity.create(100 + it, quoted = TweetEntity.create(it))
            }
            private val target = tweetList[1]
            private val retweetResponse = TweetEntity.create(
                tweetList.last().id + 1L,
                retweeted = TweetEntity.create(target.id, true)
            )
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // setup
            setupPostRetweet(target.id, retweetResponse)

            // exercise
            sut.postRetweet(target.id)

            // verify
            assertThat(tweetListItem(target.id)?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postRetweet_forQuotedTweet(): Unit = rule.runs {
            // setup
            val targetId = checkNotNull(target.quotedTweet?.id)
            setupPostRetweet(
                targetId,
                TweetEntity.create(
                    tweetList.last().id + 1,
                    retweeted = TweetEntity.create(targetId, isRetweeted = true)
                )
            )

            // exercise
            sut.postRetweet(targetId)

            // verify
            assertThat(tweetListItem(target.id)?.quoted?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet_passTweetIdOfRetweetResponse(): Unit = rule.runs {
            // setup
            setupPostRetweet(target.id, retweetResponse)
            setupPostUnretweet(retweetResponse.id, TweetEntity.create(target.id))
            val res = sut.postRetweet(target.id)

            // exercise
            sut.postUnretweet(res.id)

            // verify
            assertThat(tweetListItem(target.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            setupPostRetweet(target.id, retweetResponse)
            setupPostUnretweet(retweetResponse.id, TweetEntity.create(target.id))
            sut.postRetweet(target.id)

            // exercise
            sut.postUnretweet(target.id)

            // verify
            assertThat(tweetListItem(target.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postLike_forQuotedTweet() = rule.runs {
            // setup
            val targetId = checkNotNull(target.quotedTweet?.id)
            setupPostLike(targetId, TweetEntity.create(targetId, isFavorited = true))

            // exercise
            sut.postLike(targetId)

            // verify
            assertThat(tweetListItem(target.id)?.quoted?.isFavorited).isTrue()
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenRetweetListLoaded {
        companion object {
            private val tweetList = (0..10).map {
                TweetEntity.create(100 + it, retweeted = TweetEntity.create(it))
            }
            private val retweeted = tweetList[1]
            private val retweetedBody = checkNotNull(retweeted.retweetedTweet)
            private val retweetResponse = TweetEntity.create(
                111,
                isRetweeted = true,
                retweeted = TweetEntity.create(retweetedBody.id, true)
            )
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
            setupPostRetweet(retweeted.id, retweetResponse)
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // exercise
            sut.postRetweet(retweeted.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet_passTweetIdOfRetweetResponse(): Unit = rule.runs {
            // setup
            setupPostUnretweet(retweetResponse.id, TweetEntity.create(retweetedBody.id))
            val res = sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(res.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            setupPostUnretweet(retweetResponse.id, TweetEntity.create(retweetedBody.id))
            sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(retweeted.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenRetweetedByCurrentUserListIsLoaded {
        companion object {
            private val tweetList = (0..10).map { TweetEntity.create(it, true) }
            private val targetTweet = tweetList[1]
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
        }

        @Test
        fun postUnretweet_passOriginalTweetId(): Unit = rule.runs {
            // setup
            setupPostUnretweet(targetTweet.id, targetTweet)

            // exercise
            sut.postUnretweet(targetTweet.id)

            // verify
            assertThat(tweetListItem(targetTweet.id)?.body?.isRetweeted).isFalse()
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenRetweetListRetweetedByCurrentUserIsLoaded {
        companion object {
            private val tweetList = (0..10).map {
                TweetEntity.create(
                    100 + it, retweeted = TweetEntity.create(it, isRetweeted = true)
                )
            }
            private val targetTweet = tweetList[1]
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
        }

        @Test
        fun postUnretweet_passOriginalTweetId(): Unit = rule.runs {
            // setup
            setupPostUnretweet(targetTweet.id, targetTweet)

            // exercise
            sut.postUnretweet(targetTweet.id)

            // verify
            assertThat(tweetListItem(targetTweet.id)?.body?.isRetweeted).isFalse()
        }
    }
}

class TweetRepositoryTestRule : TestWatcher() {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val prefs = SharedPreferenceDataSource(
        app.getSharedPreferences("test_pref", Context.MODE_PRIVATE)
    )
    private val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val restClient = MockVerified.create<TweetApiClient>()

    private val currentUser = UserId(100)

    internal val sut: TweetRepository by lazy {
        TweetRepository(db.tweetDao(), prefs, restClient.mock)
    }

    override fun starting(description: Description?) {
        println("start::: $description")
        super.starting(description)
        prefs.setCurrentUserId(currentUser)
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(restClient)
            .apply(super.apply(base, description), description)
    }

    internal fun runs(block: suspend TweetRepositoryTestRule.() -> Unit): Unit = runBlocking {
        block()
    }

    internal suspend fun setupTimeline(
        userId: UserId = currentUser,
        tweetList: List<TweetEntity>
    ) = db.apply {
        val listId = listDao().addList(userId)
        val tweetListDao = TweetListDao(tweetDao())
        tweetListDao.putList(tweetList, ListQuery(QueryType.TweetQueryType.Timeline()), listId)
    }

    internal fun setupPostRetweet(targetId: TweetId, res: TweetEntity) = restClient.apply {
        coSetupResponseWithVerify(target = { mock.postRetweet(targetId) }, res = res)
    }

    internal fun setupPostUnretweet(targetId: TweetId, res: TweetEntity) = restClient.apply {
        coSetupResponseWithVerify(target = { mock.postUnretweet(targetId) }, res = res)
    }

    internal fun setupPostLike(targetId: TweetId, res: TweetEntity) = restClient.apply {
        coSetupResponseWithVerify(target = { mock.postLike(targetId) }, res)
    }

    internal suspend fun tweetListItem(tweetId: TweetId): TweetListItem? {
        return db.tweetDao().findTweetListItem(tweetId)
    }
}

internal fun TweetEntity.Companion.create(
    id: Int,
    isRetweeted: Boolean = false,
    isFavorited: Boolean = false,
    retweeted: TweetEntity? = null,
    quoted: TweetEntity? = null,
): TweetEntity = create(TweetId(id.toLong()), isRetweeted, isFavorited, retweeted, quoted)

internal fun TweetEntity.Companion.create(
    id: TweetId,
    isRetweeted: Boolean = false,
    isFavorited: Boolean = false,
    retweeted: TweetEntity? = null,
    quoted: TweetEntity? = null,
): TweetEntity = mockk<TweetEntity>(relaxed = true).also {
    every { it.id } returns id
    every { it.isFavorited } returns isFavorited
    every { it.isRetweeted } returns isRetweeted
    every { it.retweetedTweet } returns retweeted
    every { it.retweetIdByCurrentUser } returns null
    every { it.quotedTweet } returns quoted
}
