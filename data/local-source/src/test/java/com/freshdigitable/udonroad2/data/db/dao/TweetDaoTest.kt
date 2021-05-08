/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.entity.updateCursorById
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.PageOption
import com.freshdigitable.udonroad2.model.PagedResponseList
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.plus
import com.freshdigitable.udonroad2.test_common.jvm.createMock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TweetDaoTest {
    companion object {
        private val ownerUser = UserId(100)
        private fun createConversationList(
            startId: Int,
            endId: Int,
            replyToForOldest: TweetId? = null,
        ): List<TweetEntity> {
            return (startId until endId).map { id ->
                TweetEntity.createMock(
                    id = id,
                    replyTo = if (id == startId) replyToForOldest else TweetId(id - 1L)
                )
            }.sortedByDescending { it.id.value }
        }

        private fun TweetEntity.toConversation(): TweetDao.ConversationEntity =
            TweetDao.ConversationEntity(id, inReplyToTweetId)

        private fun List<TweetEntity>.mapToConversationEntity(): List<TweetDao.ConversationEntity> =
            map { it.toConversation() }
    }

    @get:Rule
    val rule = AppDatabaseFixture()

    private val sut: TweetDao by lazy { rule.db.tweetDao() }

    @Test
    fun getConversationTweetIdsByTweetId(): Unit = runBlocking {
        // setup
        val list = createConversationList(1000, 1010)
        rule.setupTweetList(ownerUser, list)

        // exercise
        val actual = sut.getConversationTweetIdsByTweetId(list.first().id)

        // verify
        assertThat(actual).containsExactly(*list.mapToConversationEntity().toTypedArray())
        assertThat(actual.first()).isEqualTo(list.first().toConversation())
    }

    @Test
    fun getConversationTweetIdsByTweetId_queryWithRetweet_then_returnWithRetweet(): Unit =
        runBlocking {
            // setup
            val list = createConversationList(1000, 1010)
            val rt = TweetEntity.createMock(list.first().id + 1, retweeted = list.first())
            rule.setupTweetList(ownerUser, list + rt)

            // exercise
            val actual = sut.getConversationTweetIdsByTweetId(rt.id)

            // verify
            val expectedFirstItem =
                TweetDao.ConversationEntity(rt.id, list.first().inReplyToTweetId)
            assertThat(actual).containsExactly(
                expectedFirstItem,
                *list.slice(1 until list.size).mapToConversationEntity().toTypedArray()
            )
            assertThat(actual.first()).isEqualTo(expectedFirstItem)
        }

    @Test
    fun getConversationTweetIdsByTweetId_queryWithLimit20_then_returnOnly20(): Unit = runBlocking {
        // setup
        val list = createConversationList(1000, 1030)
        rule.setupTweetList(ownerUser, list)

        // exercise
        val actual = sut.getConversationTweetIdsByTweetId(list.first().id, 20)

        // verify
        assertThat(actual).containsExactly(
            *list.slice(0 until 20).mapToConversationEntity().toTypedArray()
        )
        assertThat(actual.first()).isEqualTo(list.first().toConversation())
        assertThat(actual.last()).isEqualTo(list[19].toConversation())
    }

    @Test
    fun getConversationTweetIdsByTweetId_queryHalf_then_get5Items(): Unit = runBlocking {
        // setup
        val list = createConversationList(1000, 1010)
        rule.setupTweetList(ownerUser, list)
        val targetIndex = list.size / 2

        // exercise
        val actual = sut.getConversationTweetIdsByTweetId(list[targetIndex].id)

        // verify
        assertThat(actual).containsExactly(
            *list.slice(targetIndex until list.size).mapToConversationEntity().toTypedArray()
        )
        assertThat(actual.first()).isEqualTo(list[targetIndex].toConversation())
    }

    @Test
    fun getConversationTweetIdsByTweetId_queryHasNoReplyTweetId_then_getSingle(): Unit =
        runBlocking {
            // setup
            val list = createConversationList(1000, 1010)
            rule.setupTweetList(ownerUser, list)

            // exercise
            val actual = sut.getConversationTweetIdsByTweetId(list.last().id)

            // verify
            assertThat(actual).containsExactly(list.last().toConversation())
        }

    @Test
    fun getConversationTweetIdsByTweetId_queryNotToBeFoundId_then_returnEmpty(): Unit =
        runBlocking {
            // setup
            val list = createConversationList(1000, 1010)
            rule.setupTweetList(ownerUser, list)

            // exercise
            val actual = sut.getConversationTweetIdsByTweetId(TweetId(2000))

            // verify
            assertThat(actual).isEmpty()
        }
}

class AppDatabaseFixture : TestWatcher() {
    val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    )
        .allowMainThreadQueries()
        .build()

    fun setupTweetList(ownerUser: UserId, tweetEntities: List<TweetEntity>): Unit = runBlocking {
        val tweet = PagedResponseList(
            tweetEntities,
            prependCursor = tweetEntities.first().id.value + 1,
            appendCursor = tweetEntities.last().id.value - 1
        )
        val listEntity = db.listDao().run {
            val id = addList(ownerUser)
            getListById(id)
        }
        db.tweetDao().addTweetsToList(tweet, listEntity)
        val query = ListQuery(QueryType.Tweet.Timeline(), PageOption.OnInit)
        db.listDao().updateCursorById(tweet, query, listEntity.id)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        db.close()
    }
}
