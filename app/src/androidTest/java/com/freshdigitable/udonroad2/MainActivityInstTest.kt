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

import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.main.MainActivity
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import twitter4j.auth.AccessToken

@RunWith(AndroidJUnit4::class)
class MainActivityInstTest {
    @get:Rule
    val intentsTestRule = IntentsTestRule(MainActivity::class.java, false, false)

    @get:Rule
    val twitterRobot = TwitterRobot()

    @Test
    fun startOauth() {
        // setup
        val requestToken = createRequestToken(
            10000, "verified_token", "verified_secret_token", "http://localhost/hoge"
        )
        twitterRobot.setupSetOAuthAccessToken { null }
        twitterRobot.setupSetOAuthAccessToken { match { it.userId == 10000L } }
        twitterRobot.setupGetOAuthRequestToken(response = requestToken)
        twitterRobot.setupGetOAuthAccessToken(
            { any() },
            { "0123456" },
            AccessToken(requestToken.token, requestToken.tokenSecret)
        )
        twitterRobot.setupGetHomeTimeline(emptyList())

        intentsTestRule.launchActivity(null)
        checkTitle("Welcome")
        OauthRobot.Result().sendPinIsDisabled()
        intendingToAuthorizationUrl(requestToken.authorizationURL)

        // exercise
        oauth {
            clickLogin()
            inputPin("0123456")
        } result {
            sendPinIsEnabled()
        }

        oauth {
            clickSendPin()
        }
        checkTitle("Home")
    }

    @Test
    fun startTimeline() {
        // setup
        val sp = ApplicationProvider.getApplicationContext<TestApplicationBase>()
            .component
            .sharedPreferencesDao
        sp.storeAccessToken(AccessTokenEntity.create(10000, "token", "tokensecret"))
        sp.setCurrentUserId(10000)

        val countingIdlingResource = CountingIdlingResource("load_timeline")
        twitterRobot.setupSetOAuthAccessToken { any() }
        twitterRobot.setupGetHomeTimeline(emptyList()) {
            countingIdlingResource.decrement()
        }
        countingIdlingResource.increment()
        IdlingRegistry.getInstance().register(countingIdlingResource)
        intentsTestRule.launchActivity(null)

        // verify
        checkTitle("Home")
    }

    private fun checkTitle(title: String) {
        onView(
            allOf(withParent(withId(R.id.action_bar)), isAssignableFrom(TextView::class.java))
        ).check(matches(withText(title)))
    }
}
