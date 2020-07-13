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

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers.not

fun oauth(block: OauthRobot.() -> Unit): OauthRobot = OauthRobot().apply(block)

class OauthRobot {
    companion object {
        private val loginButton: ViewInteraction = onView(withId(R.id.oauth_start))
        private val pinEditText: ViewInteraction = onView(withId(R.id.oauth_pin))
        private val sendPinButton: ViewInteraction = onView(withId(R.id.oauth_send_pin))
    }

    fun clickLogin(): ViewInteraction = loginButton.perform(click())

    fun inputPin(pin: String): ViewInteraction {
        return pinEditText.perform(typeText(pin)).also {
            closeSoftKeyboard()
        }
    }

    fun clickSendPin(): ViewInteraction = sendPinButton.perform(click())

    infix fun result(block: Result.() -> Unit) {
        Result().apply(block)
    }

    class Result {
        fun sendPinIsEnabled() {
            sendPinButton.check(matches(isEnabled()))
        }

        fun sendPinIsDisabled() {
            sendPinButton.check(matches(not(isEnabled())))
        }
    }
}
