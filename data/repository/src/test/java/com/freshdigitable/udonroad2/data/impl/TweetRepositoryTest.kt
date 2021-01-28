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
import com.freshdigitable.udonroad2.data.local.SharedPreferenceDataSource
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.model.tweet.plus
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
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
            private val tweetList = (0..10).map { TweetEntity.createMock(it) }
            private val retweeted = tweetList[1]
            private val retweetResponse =
                TweetEntity.createMock(11, retweeted = TweetEntity.createMock(retweeted.id, true))
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
            setupPostUnretweet(retweetResponse.id, TweetEntity.createMock(retweeted.id))
            val res = sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(res.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            setupPostUnretweet(retweetResponse.id, TweetEntity.createMock(retweeted.id))
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
                TweetEntity.createMock(100 + it, quoted = TweetEntity.createMock(it))
            }
            private val target = tweetList[1]
            private val retweetResponse = TweetEntity.createMock(
                tweetList.last().id + 1L,
                retweeted = TweetEntity.createMock(target.id, true)
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
                TweetEntity.createMock(
                    tweetList.last().id + 1,
                    retweeted = TweetEntity.createMock(targetId, isRetweeted = true)
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
            setupPostUnretweet(retweetResponse.id, TweetEntity.createMock(target.id))
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
            setupPostUnretweet(retweetResponse.id, TweetEntity.createMock(target.id))
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
            setupPostLike(targetId, TweetEntity.createMock(targetId, isFavorited = true))

            // exercise
            sut.postLike(targetId)

            // verify
            assertThat(tweetListItem(target.id)?.quoted?.isFavorited).isTrue()
        }

        @Test
        fun findTweetListItem_forQuotedTweet() = rule.runs {
            // setup
            val targetQuotedTweet = checkNotNull(target.quotedTweet)
            val targetQuotedTweetId = targetQuotedTweet.id
            setupFetchTweet(targetQuotedTweetId, targetQuotedTweet)

            // exercise
            val actual = sut.findTweetListItem(targetQuotedTweetId)

            // verify
            assertThat(actual?.originalId).isEqualTo(targetQuotedTweetId)
        }

        @Test
        fun getTweetItemSource_forQuotedTweet() = rule.runs {
            // setup
            val targetQuotedTweetId = checkNotNull(target.quotedTweet).id

            // exercise
            val actual = sut.getTweetItemSource(targetQuotedTweetId).first()

            // verify
            assertThat(actual?.originalId).isEqualTo(targetQuotedTweetId)
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenRetweetListLoaded {
        companion object {
            private val tweetList = (0..10).map {
                TweetEntity.createMock(100 + it, retweeted = TweetEntity.createMock(it))
            }
            private val retweeted = tweetList[1]
            private val retweetedBody = checkNotNull(retweeted.retweetedTweet)
            private val retweetResponse = TweetEntity.createMock(
                111,
                isRetweeted = true,
                retweeted = TweetEntity.createMock(retweetedBody.id, true)
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
            setupPostUnretweet(retweetResponse.id, TweetEntity.createMock(retweetedBody.id))
            val res = sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(res.id)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            setupPostUnretweet(retweetResponse.id, TweetEntity.createMock(retweetedBody.id))
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
            private val tweetList = (0..10).map { TweetEntity.createMock(it, true) }
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
                TweetEntity.createMock(
                    100 + it, retweeted = TweetEntity.createMock(it, isRetweeted = true)
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
        super.starting(description)
        runBlocking {
            prefs.updateCurrentUser(AccessTokenEntity.create(currentUser, "token", "token_secret"))
        }
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(restClient)
            .apply(super.apply(base, description), description)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        db.close()
    }

    internal fun runs(block: suspend TweetRepositoryTestRule.() -> Unit): Unit = runBlocking {
        block()
    }

    internal suspend fun setupTimeline(
        userId: UserId = currentUser,
        tweetList: List<TweetEntity>
    ) = db.apply {
        val listId = listDao().addList(userId)
        val tweetListDao = TweetListDao(db)
        tweetListDao.putList(
            PagedResponseList(tweetList),
            ListQuery(QueryType.TweetQueryType.Timeline()),
            listId
        )
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

    internal fun setupFetchTweet(targetId: TweetId, res: TweetEntity) = restClient.apply {
        coSetupResponseWithVerify(target = { mock.fetchTweet(targetId) }, res)
    }

    internal suspend fun tweetListItem(tweetId: TweetId): TweetListItem? {
        return db.tweetDao().findDetailTweetListItem(tweetId, currentUser)
    }
}
