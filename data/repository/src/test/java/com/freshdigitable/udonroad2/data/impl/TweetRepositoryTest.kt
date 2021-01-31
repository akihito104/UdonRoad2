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
import com.freshdigitable.udonroad2.data.local.TweetLocalDataSource
import com.freshdigitable.udonroad2.data.restclient.AppTwitter
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetListItem
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.TwitterRobotBase
import com.freshdigitable.udonroad2.test_common.createStatus
import com.freshdigitable.udonroad2.test_common.createUser
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import twitter4j.Status
import twitter4j.Twitter

@RunWith(Enclosed::class)
class TweetRepositoryTest {

    @RunWith(AndroidJUnit4::class)
    class WhenPlainTweetListLoaded {
        companion object {
            private val tweetList = (0..10).map { TweetEntity.createMock(it) }
            private val retweeted = tweetList[1]
            private val retweetResponse =
                createStatus(
                    11,
                    user = createUser(200, "user2", "user2"),
                    retweetedStatus = createStatus(
                        retweeted.id.value,
                        user = createUser(100, "user1", "user1"),
                        isRetweeted = true
                    )
                )
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
            restClient.setupPostRetweet(retweeted.id, retweetResponse)
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // exercise
            sut.updateRetweet(retweeted.id, true)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet_passTweetIdOfRetweetResponse(): Unit = rule.runs {
            // setup
            restClient.setupPostUnretweet(retweetResponse.id, createStatus(retweeted.id.value))
            val res = sut.updateRetweet(retweeted.id, true)

            // exercise
            sut.updateRetweet(res.id, false)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }

        @Ignore
        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            restClient.setupPostUnretweet(retweetResponse.id, createStatus(retweeted.id.value))
            sut.updateRetweet(retweeted.id, true)

            // exercise
            sut.updateRetweet(retweeted.id, false)

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
            private val retweetResponse = createStatus(
                tweetList.last().id.value + 1L,
                retweetedStatus = createStatus(target.id.value, isRetweeted = true)
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
            restClient.setupPostRetweet(target.id, retweetResponse)

            // exercise
            sut.updateRetweet(target.id, true)

            // verify
            assertThat(tweetListItem(target.id)?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postRetweet_forQuotedTweet(): Unit = rule.runs {
            // setup
            val targetId = checkNotNull(target.quotedTweet?.id)
            restClient.setupPostRetweet(
                targetId,
                createStatus(
                    tweetList.last().id.value + 1,
                    retweetedStatus = createStatus(targetId.value, isRetweeted = true)
                )
            )

            // exercise
            sut.updateRetweet(targetId, true)

            // verify
            assertThat(tweetListItem(target.id)?.quoted?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet_passTweetIdOfRetweetResponse(): Unit = rule.runs {
            // setup
            restClient.setupPostRetweet(target.id, retweetResponse)
            restClient.setupPostUnretweet(retweetResponse.id, createStatus(target.id.value))
            val res = sut.updateRetweet(target.id, true)

            // exercise
            sut.updateRetweet(res.id, false)

            // verify
            assertThat(tweetListItem(target.id)?.body?.isRetweeted).isFalse()
        }

        @Ignore
        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            restClient.setupPostRetweet(target.id, retweetResponse)
            restClient.setupPostUnretweet(retweetResponse.id, createStatus(target.id.value))
            sut.updateRetweet(target.id, true)

            // exercise
            sut.updateRetweet(target.id, false)

            // verify
            assertThat(tweetListItem(target.id)?.body?.isRetweeted).isFalse()
        }

        @Test
        fun postLike_forQuotedTweet() = rule.runs {
            // setup
            val targetId = checkNotNull(target.quotedTweet?.id)
            restClient.setupPostLike(targetId, createStatus(targetId.value, isFavorited = true))

            // exercise
            sut.updateLike(targetId, true)

            // verify
            assertThat(tweetListItem(target.id)?.quoted?.isFavorited).isTrue()
        }

        @Test
        fun findTweetListItem_forQuotedTweet() = rule.runs {
            // setup
            val targetQuotedTweet = checkNotNull(target.quotedTweet)
            val targetQuotedTweetId = targetQuotedTweet.id
            restClient.setupFetchTweet(targetQuotedTweetId, createStatus(targetQuotedTweetId.value))

            // exercise
            val actual = sut.findDetailTweetItem(targetQuotedTweetId)

            // verify
            assertThat(actual?.originalId).isEqualTo(targetQuotedTweetId)
        }

        @Test
        fun getTweetItemSource_forQuotedTweet() = rule.runs {
            // setup
            val targetQuotedTweetId = checkNotNull(target.quotedTweet).id

            // exercise
            val actual = sut.getDetailTweetItemSource(targetQuotedTweetId).first()

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
            private val retweetResponse = createStatus(
                111,
                isRetweeted = true,
                retweetedStatus = createStatus(retweetedBody.id.value, isRetweeted = true)
            )
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            setupTimeline(tweetList = tweetList)
            restClient.setupPostRetweet(retweeted.id, retweetResponse)
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // exercise
            sut.updateRetweet(retweeted.id, true)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet_passTweetIdOfRetweetResponse(): Unit = rule.runs {
            // setup
            restClient.setupPostUnretweet(
                retweetResponse.id,
                createStatus(retweetedBody.id.value)
            )
            val res = sut.updateRetweet(retweeted.id, true)

            // exercise
            sut.updateRetweet(res.id, false)

            // verify
            assertThat(tweetListItem(retweeted.id)?.body?.isRetweeted).isFalse()
        }

        @Ignore
        @Test
        fun postUnretweet_passTweetIdOfOriginal(): Unit = rule.runs {
            // setup
            restClient.setupPostUnretweet(
                retweetResponse.id,
                createStatus(retweetedBody.id.value)
            )
            sut.updateRetweet(retweeted.id, true)

            // exercise
            sut.updateRetweet(retweeted.id, false)

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
            restClient.setupPostUnretweet(
                targetTweet.id.value,
                createStatus(targetTweet.id.value, isRetweeted = targetTweet.isRetweeted)
            )

            // exercise
            sut.updateRetweet(targetTweet.id, false)

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
            restClient.setupPostUnretweet(
                targetTweet.id.value, createStatus(
                    targetTweet.id.value, retweetedStatus = createStatus(
                        requireNotNull(targetTweet.retweetedTweet).id.value,
                        isRetweeted = requireNotNull(targetTweet.retweetedTweet).isRetweeted
                    )
                )
            )

            // exercise
            sut.updateRetweet(targetTweet.id, false)

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
    val restClient = TwitterMock()

    private val currentUser = UserId(100)

    private val local = TweetLocalDataSource(db, prefs)
    internal val sut: TweetRepository by lazy {
        TweetRepository(local, restClient.tweetApi)
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

    internal suspend fun tweetListItem(tweetId: TweetId): TweetListItem? {
        return local.findDetailTweetItem(tweetId)
    }
}

class TwitterMock : TwitterRobotBase(), TestRule {
    private val restClient = MockVerified.create<Twitter>()
    override val twitter: Twitter = restClient.mock
    val tweetApi = TweetApiClient(AppTwitter(twitter))

    internal fun setupPostRetweet(targetId: TweetId, res: Status) =
        restClient.coSetupResponseWithVerify(
            target = { twitter.retweetStatus(targetId.value) },
            res = res
        )

    internal fun setupPostUnretweet(targetId: Long, res: Status) =
        restClient.coSetupResponseWithVerify(
            target = { twitter.unRetweetStatus(targetId) },
            res = res
        )

    internal fun setupPostLike(targetId: TweetId, res: Status) =
        restClient.coSetupResponseWithVerify(
            target = { twitter.createFavorite(targetId.value) },
            res
        )

    internal fun setupFetchTweet(targetId: TweetId, res: Status) =
        restClient.coSetupResponseWithVerify(target = { twitter.showStatus(targetId.value) }, res)

    override fun apply(base: Statement?, description: Description?): Statement =
        restClient.apply(requireNotNull(base), description)
}
