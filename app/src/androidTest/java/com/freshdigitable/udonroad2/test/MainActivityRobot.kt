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

package com.freshdigitable.udonroad2.test

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.freshdigitable.udonroad2.R
import org.hamcrest.CoreMatchers.allOf

fun onMainActivity(block: MainActivityRobot.() -> Unit) {
    MainActivityRobot().apply(block)
}

class MainActivityRobot : ActivityRobot {
    companion object {
        private val fab: ViewInteraction = onView(withId(R.id.main_fab))
        private val verifyRobot = Verify()
    }

    fun showDetail() {
        fab.perform(swipeLeft())
    }

    fun showConversation() {
        fab.perform(
            actionWithAssertions(
                GeneralSwipeAction(
                    Swipe.FAST,
                    { v ->
                        val coord = GeneralLocation.CENTER.calculateCoordinates(v)
                        floatArrayOf(coord[0] - 0.083f * v.width, coord[1] - 0.083f * v.height)
                    },
                    GeneralLocation.BOTTOM_LEFT,
                    Press.FINGER
                )
            )
        )
    }

    fun checkFabIsDisplayed() {
        verifyRobot.fabIsDisplayed()
    }

    fun checkActionBarTitle(@StringRes titleRes: Int) {
        verifyRobot.actionBarTitle(titleRes)
    }

    infix fun verify(block: Verify.() -> Unit) {
        verifyRobot.apply(block)
    }

    class Verify : ActivityRobot.VerifyRobot, FabVerify by FabVerify.get(fab) {
        override fun actionBarTitle(titleRes: Int) {
            onView(
                allOf(
                    withParent(withId(R.id.main_toolbar)),
                    isAssignableFrom(TextView::class.java)
                )
            ).check(matches(withText(titleRes)))
        }
    }
}
