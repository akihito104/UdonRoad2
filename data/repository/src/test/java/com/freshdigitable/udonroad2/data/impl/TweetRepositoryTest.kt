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
            private val tweetList = (0..10).map { createTweetEntity(it) }
            private val retweeted = tweetList[1]
            private val retweetResponse =
                createTweetEntity(11, isRetweeted = true, retweeted = retweeted)
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            db.apply {
                val listId = listDao().addList(currentUser)
                TweetListDao(tweetDao()).apply {
                    putList(tweetList, ListQuery(QueryType.TweetQueryType.Timeline()), listId)
                }
            }
            restClient.apply {
                val retweetedId = retweeted.id
                coSetupResponseWithVerify(
                    target = { mock.postRetweet(retweetedId) },
                    res = retweetResponse
                )
            }
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // exercise
            sut.postRetweet(retweeted.id)

            // verify
            val actual = db.tweetDao().findTweetListItem(retweeted.id)
            assertThat(actual?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet(): Unit = rule.runs {
            // setup
            val retweetId = retweetResponse.id
            restClient.apply {
                coSetupResponseWithVerify(
                    target = { mock.postUnretweet(retweetId) },
                    res = createTweetEntity(retweeted.id.value.toInt())
                )
            }
            sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(retweetId)

            // verify
            val actual = db.tweetDao().findTweetListItem(retweeted.id)
            assertThat(actual?.body?.isRetweeted).isFalse()
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenRetweetListLoaded {
        companion object {
            private val tweetList = (0..10).map {
                createTweetEntity(100 + it, retweeted = createTweetEntity(it))
            }
            private val retweeted = tweetList[1]
            private val retweetedBody = checkNotNull(retweeted.retweetedTweet)
            val retweetResponse =
                createTweetEntity(111, isRetweeted = true, retweeted = retweetedBody)
        }

        @get:Rule
        val rule = TweetRepositoryTestRule()

        @Before
        fun setup(): Unit = rule.runs {
            db.apply {
                val listId = listDao().addList(currentUser)
                TweetListDao(tweetDao()).apply {
                    putList(tweetList, ListQuery(QueryType.TweetQueryType.Timeline()), listId)
                }
            }
            restClient.apply {
                val retweetedId = retweeted.id
                coSetupResponseWithVerify(
                    target = { mock.postRetweet(retweetedId) }, res = retweetResponse
                )
            }
        }

        @Test
        fun postRetweet(): Unit = rule.runs {
            // exercise
            sut.postRetweet(retweeted.id)

            // verify
            val actual = db.tweetDao().findTweetListItem(retweeted.id)
            assertThat(actual?.body?.isRetweeted).isTrue()
        }

        @Test
        fun postUnretweet(): Unit = rule.runs {
            // setup
            restClient.apply {
                val id = retweetResponse.id
                coSetupResponseWithVerify(
                    target = { mock.postUnretweet(id) },
                    res = createTweetEntity(retweetedBody.id.value.toInt())
                )
            }
            sut.postRetweet(retweeted.id)

            // exercise
            sut.postUnretweet(retweeted.id)

            // verify
            val actual = db.tweetDao().findTweetListItem(retweeted.id)
            assertThat(actual?.body?.isRetweeted).isFalse()
        }
    }
}

class TweetRepositoryTestRule : TestWatcher() {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val prefs = SharedPreferenceDataSource(
        app.getSharedPreferences("test_pref", Context.MODE_PRIVATE)
    )
    internal val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    internal val restClient = MockVerified.create<TweetApiClient>()

    internal val currentUser = UserId(100)

    internal val sut: TweetRepository by lazy {
        TweetRepository(db.tweetDao(), prefs, restClient.mock)
    }

    override fun starting(description: Description?) {
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
}

internal fun createTweetEntity(
    id: Int,
    isRetweeted: Boolean = false,
    retweeted: TweetEntity? = null,
): TweetEntity = mockk<TweetEntity>(relaxed = true).also {
    every { it.id } returns TweetId(id.toLong())
    every { it.isFavorited } returns false
    every { it.isRetweeted } returns isRetweeted
    every { it.retweetedTweet } returns retweeted
}
