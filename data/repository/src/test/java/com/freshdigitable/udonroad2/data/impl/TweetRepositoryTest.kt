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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TweetRepositoryTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
        .build()
    private val currentUser = UserId(100)
    private val prefs = SharedPreferenceDataSource(
        app.getSharedPreferences("test_pref", Context.MODE_PRIVATE)
    ).apply {
        setCurrentUserId(currentUser)
    }

    @get:Rule
    val restClient = MockVerified.create<TweetApiClient>()

    private val sut: TweetRepository by lazy {
        TweetRepository(db.tweetDao(), prefs, restClient.mock)
    }

    @Test
    fun postRetweet() = runBlocking {
        // setup
        val tweetList = (0..10).map { createTweetEntity(it) }
        db.apply {
            val listId = listDao().addList(currentUser)
            TweetListDao(tweetDao()).apply {
                putList(tweetList, ListQuery(QueryType.TweetQueryType.Timeline()), listId)
            }
        }
        restClient.apply {
            val retweeted = tweetList[1]
            val retweetedId = retweeted.id
            coSetupResponseWithVerify(
                target = { mock.postRetweet(retweetedId) },
                res = createTweetEntity(11, isRetweeted = true, retweeted = retweeted)
            )
        }

        // exercise
        sut.postRetweet(TweetId(1))

        // verify
        val retweeted = db.tweetDao().findTweetListItem(TweetId(1))
        assertThat(retweeted?.body?.isRetweeted).isTrue()
    }

    @Test
    fun postUnretweet() = runBlocking {
        // setup
        val tweetList = (0..10).map { createTweetEntity(it) }
        db.apply {
            val listId = listDao().addList(currentUser)
            TweetListDao(tweetDao()).apply {
                putList(tweetList, ListQuery(QueryType.TweetQueryType.Timeline()), listId)
            }
        }
        restClient.apply {
            val retweeted = tweetList[1]
            val retweetedId = retweeted.id
            coSetupResponseWithVerify(
                target = { mock.postRetweet(retweetedId) },
                res = createTweetEntity(11, isRetweeted = true, retweeted = retweeted)
            )
            coSetupResponseWithVerify(
                target = { mock.postUnretweet(TweetId(11)) },
                res = createTweetEntity(retweeted.id.value.toInt())
            )
        }
        sut.postRetweet(TweetId(1))

        // exercise
        sut.postUnretweet(TweetId(11))

        // verify
        val unretweeted = db.tweetDao().findTweetListItem(TweetId(1))
        assertThat(unretweeted?.body?.isRetweeted).isFalse()
    }

    private fun createTweetEntity(
        id: Int,
        isRetweeted: Boolean = false,
        retweeted: TweetEntity? = null,
    ): TweetEntity = mockk<TweetEntity>(relaxed = true).also {
        every { it.id } returns TweetId(id.toLong())
        every { it.isFavorited } returns false
        every { it.isRetweeted } returns isRetweeted
        every { it.retweetedTweet } returns retweeted
    }
}
