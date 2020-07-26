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

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freshdigitable.udonroad2.main.MainActivity
import com.freshdigitable.udonroad2.model.AccessTokenEntity
import com.freshdigitable.udonroad2.test.intendingToAuthorizationUrl
import com.freshdigitable.udonroad2.test.mainList
import com.freshdigitable.udonroad2.test.oauth
import com.freshdigitable.udonroad2.test.onMainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import twitter4j.auth.AccessToken
import java.util.Date

@RunWith(Enclosed::class)
class MainActivityInstTest {
    @RunWith(AndroidJUnit4::class)
    class WhenOAuthIsNeededOnLaunch {
        @get:Rule
        val intentsTestRule = IntentsTestRule(MainActivity::class.java, false, false)

        @get:Rule
        val twitterRobot = TwitterRobot()

        @Test
        fun startOauth(): Unit = onMainActivity {
            // setup
            val requestToken = createRequestToken(
                10000, "verified_token", "verified_secret_token"
            )
            twitterRobot.setupSetOAuthAccessToken { null }
            twitterRobot.setupSetOAuthAccessToken { match { it.userId == 10000L } }
            twitterRobot.setupGetOAuthRequestToken(response = requestToken)
            twitterRobot.setupGetOAuthAccessToken(
                { any() },
                { "0123456" },
                AccessToken(requestToken.token, requestToken.tokenSecret)
            )
            twitterRobot.setupGetHomeTimeline(response = emptyList())

            intentsTestRule.launchActivity(null)
            checkActionBarTitle(R.string.title_oauth)
            oauth { checkSendPinIsDisabled() }
            intendingToAuthorizationUrl(requestToken.authorizationURL)

            // exercise
            oauth {
                clickLogin()
                inputPin("0123456")
            } verify {
                sendPinIsEnabled()
            }

            oauth {
                clickSendPin()
            }

            // verify
            verify {
                actionBarTitle("Home")
            }
        }
    }

    @RunWith(AndroidJUnit4::class)
    class WhenLaunchWithAccessToken {
        @get:Rule
        val intentsTestRule = IntentsTestRule(MainActivity::class.java, false, false)

        @get:Rule
        val twitterRobot = TwitterRobot()

        @Before
        fun setup(): Unit = onMainActivity {
            val sp = ApplicationProvider.getApplicationContext<TestApplicationBase>()
                .component
                .sharedPreferencesDao
            sp.storeAccessToken(AccessTokenEntity.create(10000, "token", "tokensecret"))
            sp.setCurrentUserId(10000)

            val countingIdlingResource = CountingIdlingResource("load_timeline")
            twitterRobot.setupSetOAuthAccessToken { any() }
            val user = createUser(2000, "user2000", "_user2000")
            twitterRobot.setupGetHomeTimeline(response = (0 until 10).map {
                createStatus(
                    100L + it,
                    "tweet: $it",
                    user,
                    Date(100000L + it)
                )
            }) {
                countingIdlingResource.decrement()
            }
            twitterRobot.setupGetHomeTimeline({ any() }, emptyList())
            countingIdlingResource.increment()
            IdlingRegistry.getInstance().register(countingIdlingResource)
            intentsTestRule.launchActivity(null)
            checkActionBarTitle(R.string.title_home)
        }

        @Test
        fun swipeFabAndThenMoveToDetailOfSelectedTweet(): Unit = onMainActivity {
            // exercise
            mainList {
                clickListItemOf(0)
            } verify {
                checkFabIsDisplayed()
                stateIsSelectedOnItemOf(0)
            }
            showDetail()

            // verify
            verify {
                fabIsNotDisplayed()
                actionBarTitle("Tweet")
            }
        }

        @Test
        fun backFromDetailAndThenTweetIsSelected(): Unit = onMainActivity {
            // setup
            mainList {
                clickListItemOf(0)
            } verify {
                checkFabIsDisplayed()
                stateIsSelectedOnItemOf(0)
            }
            showDetail()

            // exercise
            pressBack()

            // verify
            mainList() verify {
                stateIsSelectedOnItemOf(0)
            }
            verify {
                fabIsDisplayed()
            }
        }
    }
}
