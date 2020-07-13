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

package com.freshdigitable.udonroad2

import android.app.Instrumentation
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.main.MainActivity
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

@RunWith(AndroidJUnit4::class)
class MainActivityInstTest {
    @get:Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java, false, false)

    @get:Rule
    val twitterRobot = TwitterRobot()

    @Test
    fun testLaunch() {
        intentsTestRule.launchActivity(null)
        onView(withId(R.id.oauth_start)).check(matches(isDisplayed()))
    }

    @Test
    fun testStartOauth() {
        // setup
        twitterRobot.setupSetOAuthAccessToken { null }
        twitterRobot.setupSetOAuthAccessToken { match { it.userId == 10000L } }
        twitterRobot.setupGetOAuthRequestToken({ "oob" }, mockk<RequestToken>().apply {
            every { token } returns "10000-verified_token"
            every { tokenSecret } returns "verified_secret_token"
            every { authorizationURL } returns "http://localhost/hoge"
        })
        twitterRobot.setupGetOAuthAccessToken(
            { any() },
            { "0123456" },
            AccessToken("10000-verified_token", "verified_secret_token")
        )
        twitterRobot.setupGetHomeTimeline(emptyList())

        intentsTestRule.launchActivity(null)
        onView(withId(R.id.oauth_start)).check(matches(isDisplayed()))
        intending(hasData(Uri.parse("http://localhost/hoge"))).respondWithFunction {
            Instrumentation.ActivityResult(0, null)
        }

        // exercise
        onView(withId(R.id.oauth_start)).perform(click())
        onView(withId(R.id.oauth_pin)).perform(typeText("0123456"))
        onView(withId(R.id.oauth_send_pin)).perform(click())
    }
}
