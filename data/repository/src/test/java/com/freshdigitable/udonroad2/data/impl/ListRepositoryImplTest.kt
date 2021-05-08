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

package com.freshdigitable.udonroad2.data.impl

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.data.RemoteListDataSource
import com.freshdigitable.udonroad2.data.db.AppDatabase
import com.freshdigitable.udonroad2.data.db.dao.TweetListDao
import com.freshdigitable.udonroad2.data.restclient.AppTwitter
import com.freshdigitable.udonroad2.data.restclient.TimelineRemoteDataSource
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.test_common.MockVerified
import com.freshdigitable.udonroad2.test_common.TwitterRobotBase
import com.freshdigitable.udonroad2.test_common.createStatus
import com.freshdigitable.udonroad2.test_common.createUser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import twitter4j.Twitter
import java.util.Date

@RunWith(AndroidJUnit4::class)
class ListRepositoryImplTest {
    @get:Rule
    internal val rule = ListRepositoryImplTestRule()

    @Test
    fun init() = rule.runs {
        // verify
        assertThat(sut).isNotNull()
    }

    @Ignore("because of the test makes the test process stopping")
    @Test
    fun prependList() = rule.runs {
        // setup
        val listId = db.listDao().addList(UserId(1000))
        val user = createUser(400, "user400", "user400")
        twitterRule.setupGetHomeTimeline(
            response = (0L until 10L).map {
                createStatus(it, "text: $it", user, Date(100000 + it))
            }
        )

        // exercise
        sut.prependList(QueryType.Tweet.Timeline(), listId)

        // verify
        assertThat(sut).isNotNull()
    }
}

class TwitterRuleImpl : TwitterRobotBase() {
    private val twitterMock = MockVerified.create<Twitter>()
    override val twitter: Twitter = twitterMock.mock
    val api = AppTwitter(twitter)

    override fun apply(base: Statement?, description: Description?): Statement {
        return twitterMock.apply(super.apply(base, description), description)
    }
}

internal class ListRepositoryImplTestRule : TestWatcher() {
    val twitterRule = TwitterRuleImpl()
    private val tweetApi = MockVerified.create<TweetApiClient>()

    val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    )
        .allowMainThreadQueries()
        .build()
    private val local = TweetListDao(db)
    private val remote = TimelineRemoteDataSource(twitterRule.api, tweetApi.mock)

    val sut: ListRepositoryImpl<QueryType.Tweet, TweetEntity> by lazy {
        ListRepositoryImpl(local, remote as RemoteListDataSource<QueryType.Tweet, TweetEntity>)
    }

    override fun apply(base: Statement?, description: Description?): Statement {
        return RuleChain.outerRule(InstantTaskExecutorRule())
            .around(twitterRule)
            .apply(super.apply(base, description), description)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        db.close()
    }

    fun runs(block: suspend ListRepositoryImplTestRule.() -> Unit): Unit = runBlocking { block() }
}
