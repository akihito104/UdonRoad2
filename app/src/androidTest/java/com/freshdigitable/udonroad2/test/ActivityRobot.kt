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
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.freshdigitable.udonroad2.R
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf

interface ActivityRobot {
    companion object {
        val actionBarTitle: ViewInteraction
            get() = onView(
                allOf(
                    withParent(withId(R.id.action_bar)),
                    isAssignableFrom(TextView::class.java)
                )
            )
    }

    interface VerifyRobot {
        fun actionBarTitle(@StringRes titleRes: Int) {
            actionBarTitle.check(matches(withText(titleRes)))
        }
    }

    fun pressBack() {
        Espresso.pressBack()
    }
}

interface FabVerify {

    fun fabIsDisplayed()
    fun fabIsNotDisplayed()

    companion object {
        fun get(id: Int): FabVerify = get(onView(withId(id)))
        fun get(fab: ViewInteraction): FabVerify = Impl(fab)
    }

    private class Impl(private val fab: ViewInteraction) : FabVerify {

        override fun fabIsDisplayed() {
            fab.check(matches(ViewMatchers.isDisplayed()))
        }

        override fun fabIsNotDisplayed() {
            fab.check(matches(CoreMatchers.not(ViewMatchers.isDisplayed())))
        }
    }
}
