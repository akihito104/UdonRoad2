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
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.UserId
import com.freshdigitable.udonroad2.test.TwitterRobot
import com.freshdigitable.udonroad2.test.createStatus
import com.freshdigitable.udonroad2.test.createUser
import com.freshdigitable.udonroad2.test.mainList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class UserActivityInstTest {
    @get:Rule
    val activityTestRule = ActivityTestRule(UserActivity::class.java, true, false)

    @get:Rule
    val twitterRobot = TwitterRobot()

    @Test
    fun tweetItemClicked_then_fabIsVisible() {
        val tweetingUser = TweetingUserImpl(UserId(1000), "user1", "user1", "")
        val user = createUser(tweetingUser.id.value, tweetingUser.name, tweetingUser.screenName)
        twitterRobot.setupShowUser(user)
        twitterRobot.setupRelationships(UserId(100), tweetingUser.id)
        twitterRobot.setupGetUserTimeline(tweetingUser.id, response = (0 until 10).map {
            createStatus(100L + it, "tweet: $it", user, Date(100000L + it))
        })
        twitterRobot.setupGetUserTimeline(tweetingUser.id, { any() }, emptyList())
        twitterRobot.setupGetFollowersList(tweetingUser.id, emptyList())

        val context = ApplicationProvider.getApplicationContext<Context>()
        activityTestRule.launchActivity(UserActivity.getIntent(context, tweetingUser))

        mainList {
            clickListItemOf(0)
        } verify {
            stateIsSelectedOnItemOf(0)
            onView(withId(R.id.user_fab)).check(matches(isDisplayed()))
        }
    }
}

data class TweetingUserImpl(
    override val id: UserId,
    override val name: String,
    override val screenName: String,
    override val iconUrl: String
) : TweetingUser
