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

package com.freshdigitable.udonroad2.user

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.TestApplicationBase
import com.freshdigitable.udonroad2.data.restclient.ext.toEntity
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.model.TweetId
import com.freshdigitable.udonroad2.model.UserId
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.test.TwitterRobot
import com.freshdigitable.udonroad2.test.intendedWithExtras
import com.freshdigitable.udonroad2.test.intendingWithExtras
import com.freshdigitable.udonroad2.test.mainList
import com.freshdigitable.udonroad2.test.waitForActivity
import com.freshdigitable.udonroad2.test_common.createStatus
import com.freshdigitable.udonroad2.test_common.createUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import twitter4j.MediaEntity
import java.util.Date

@RunWith(AndroidJUnit4::class)
class UserActivityInstTest {
    @get:Rule
    val activityTestRule = IntentsTestRule(UserActivity::class.java, true, false)

    @get:Rule
    val twitterRobot = TwitterRobot()

    private val authenticatedUserId = UserId(3000)

    @Before
    fun setup() {
        val tweetingUser = TweetUserItemImpl(UserId(1000), "user1", "user1", "")
        val user = createUser(tweetingUser.id.value, tweetingUser.name, tweetingUser.screenName)
        twitterRobot.setupShowUser(user)
        twitterRobot.setupRelationships(authenticatedUserId, tweetingUser.id)
        twitterRobot.setupGetUserTimeline(
            tweetingUser.id,
            response = (0 until 10).map {
                createStatus(
                    100L + it,
                    "tweet: $it",
                    user,
                    Date(100000L + it),
                    arrayOf(
                        mockk<MediaEntity>(relaxed = true).apply {
                            every { id } returns (3000L + it)
                            every { type } returns "photo"
                            every { sizes } returns mapOf()
                        }
                    )
                )
            }
        )
        twitterRobot.setupGetUserTimeline(
            tweetingUser.id,
            pagingBlock = { match { it.maxId in 0..100 } },
            response = emptyList()
        )
        twitterRobot.setupGetFollowersList(tweetingUser.id, emptyList())

        val context = ApplicationProvider.getApplicationContext<TestApplicationBase>()
        context.component.apply {
            sharedPreferencesDao.apply {
                runBlocking {
                    val accessToken =
                        AccessTokenEntity.create(authenticatedUserId, "token", "token_secret")
                    updateCurrentUser(accessToken)
                }
            }
            userDao.apply {
                runBlocking {
                    val authedUser = createUser(authenticatedUserId.value, "user2", "User2")
                    addUser(authedUser.toEntity())
                }
            }
        }
        activityTestRule.launchActivity(UserActivity.getIntent(context, tweetingUser))
    }

    @Test
    fun tweetItemClicked_then_fabIsVisible(): Unit = onUserActivity {
        mainList {
            waitForListItem {
                clickListItemOf(0)
            }
        } verify {
            stateIsSelectedOnItemOf(0)
        }
    } verify {
        fabIsDisplayed()
    }

    @Test
    fun switchTab_then_fabIsNotVisible(): Unit = onUserActivity {
        twitterRobot.setupGetFavorites(userId = UserId(1000), response = emptyList())
        twitterRobot.setupGetUserListMemberships(UserId(1000), response = emptyList())
        twitterRobot.setupGetSearchList(response = emptyList())
        mainList {
            clickListItemOf(0)
        } verify {
            stateIsSelectedOnItemOf(0)
        }

        clickPagerTabWithPosition(4)
    } verify {
        fabIsNotDisplayed()
    }

    @Test
    fun returnFirstTab_then_fabIsVisible(): Unit = onUserActivity {
        twitterRobot.setupGetFavorites(userId = UserId(1000), response = emptyList())
        twitterRobot.setupGetUserListMemberships(UserId(1000), response = emptyList())
        twitterRobot.setupGetSearchList(response = emptyList())
        mainList {
            waitForListItem {
                clickListItemOf(0)
            }
        } verify {
            stateIsSelectedOnItemOf(0)
        }

        clickPagerTabWithPosition(4)
        clickPagerTabWithPosition(0)
    } verify {
        fabIsDisplayed()
    }

    @Test
    fun clickMedia_then_sendToLaunchMediaActivityIntent() {
        val mediaActivityArgs = MediaActivityArgs(TweetId(109), 0).toBundle()
        intendingWithExtras(mediaActivityArgs)

        onUserActivity {
            waitForListItem {
                mainList {
                    clickMediaInListItemOf(0)
                }
            }
        } verify {
            intendedWithExtras(mediaActivityArgs)
            fabIsDisplayed()
        }
    }

    @Ignore("passed manual test")
    @Test
    fun returnFirstTabAndClickMedia_then_sendToLaunchMediaActivityIntent() {
        twitterRobot.setupGetFavorites(userId = UserId(1000), response = emptyList())
        twitterRobot.setupGetUserListMemberships(UserId(1000), response = emptyList())
        twitterRobot.setupGetSearchList(response = emptyList())
        val mediaActivityArgs = MediaActivityArgs(TweetId(109), 0).toBundle()
        intendingWithExtras(mediaActivityArgs)

        onUserActivity {
            clickPagerTabWithPosition(4)
            clickPagerTabWithPosition(0)

            mainList {
                clickMediaInListItemOf(0)
            }
        } verify {
            intendedWithExtras(mediaActivityArgs)
            fabIsDisplayed()
        }
    }
}

data class TweetUserItemImpl(
    override val id: UserId,
    override val name: String,
    override val screenName: String,
    override val iconUrl: String,
    override val isVerified: Boolean = false,
    override val isProtected: Boolean = false,
) : TweetUserItem

fun waitForListItem(block: () -> Unit) {
    waitForActivity<UserActivity>(
        onActivity = { it.findViewById<RecyclerView>(R.id.main_list).childCount > 0 },
        afterTask = block
    )
}
