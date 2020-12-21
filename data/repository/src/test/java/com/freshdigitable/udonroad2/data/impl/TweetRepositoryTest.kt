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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TweetRepositoryTest {
    @get:Rule
    val restClient = MockVerified.create<TweetApiClient>()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val prefs = SharedPreferenceDataSource(
        app.getSharedPreferences("test_pref", Context.MODE_PRIVATE)
    )
    private val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    private val tweetList = (0..10).map { createTweetEntity(it) }
    private val retweeted = tweetList[1]
    private val currentUser = UserId(100)

    private val sut: TweetRepository by lazy {
        TweetRepository(db.tweetDao(), prefs, restClient.mock)
    }

    @Before
    fun setup(): Unit = runBlocking {
        prefs.setCurrentUserId(currentUser)
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
                res = createTweetEntity(11, isRetweeted = true, retweeted = retweeted)
            )
        }
    }

    @Test
    fun postRetweet(): Unit = runBlocking {
        // exercise
        sut.postRetweet(retweeted.id)

        // verify
        val retweeted = db.tweetDao().findTweetListItem(retweeted.id)
        assertThat(retweeted?.body?.isRetweeted).isTrue()
    }

    @Test
    fun postUnretweet(): Unit = runBlocking {
        // setup
        val retweetId = TweetId(11)
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
        val unretweeted = db.tweetDao().findTweetListItem(retweeted.id)
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
