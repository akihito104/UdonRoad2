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

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaActivityArgs
import com.freshdigitable.udonroad2.model.tweet.TweetId
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test.TwitterRobot
import com.freshdigitable.udonroad2.test.createStatus
import com.freshdigitable.udonroad2.test.createUser
import com.freshdigitable.udonroad2.test.intendedWithExtras
import com.freshdigitable.udonroad2.test.intendingWithExtras
import com.freshdigitable.udonroad2.test.mainList
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
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

    @Before
    fun setup() {
        val tweetingUser = TweetingUserImpl(UserId(1000), "user1", "user1", "")
        val user = createUser(tweetingUser.id.value, tweetingUser.name, tweetingUser.screenName)
        twitterRobot.setupShowUser(user)
        twitterRobot.setupRelationships(UserId(100), tweetingUser.id)
        twitterRobot.setupGetUserTimeline(tweetingUser.id, response = (0 until 10).map {
            createStatus(
                100L + it,
                "tweet: $it",
                user,
                Date(100000L + it),
                arrayOf(mockk<MediaEntity>(relaxed = true).apply {
                    every { id } returns (3000L + it)
                    every { type } returns "photo"
                    every { sizes } returns mapOf()
                })
            )
        })
        twitterRobot.setupGetUserTimeline(tweetingUser.id, { any() }, emptyList())
        twitterRobot.setupGetFollowersList(tweetingUser.id, emptyList())

        val context = ApplicationProvider.getApplicationContext<Context>()
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
            clickListItemOf(0)
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

data class TweetingUserImpl(
    override val id: UserId,
    override val name: String,
    override val screenName: String,
    override val iconUrl: String
) : TweetingUser

fun waitForListItem(block: () -> Unit) {
    waitForActivity<UserActivity>(
        onActivity = { it.findViewById<RecyclerView>(R.id.main_list).childCount > 0 },
        afterTask = block
    )
}