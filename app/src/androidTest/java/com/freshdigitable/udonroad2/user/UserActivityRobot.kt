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

import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.test.ActivityRobot
import com.freshdigitable.udonroad2.test.FabVerify
import com.google.android.material.tabs.TabLayout
import org.hamcrest.Description
import org.hamcrest.Matcher

fun onUserActivity(block: UserActivityRobot.() -> Unit): UserActivityRobot {
    return UserActivityRobot().apply(block)
}

class UserActivityRobot : ActivityRobot {
    companion object {
        private val fab: ViewInteraction = onView(withId(R.id.user_fab))
    }

    fun clickPagerTabWithPosition(position: Int): ViewInteraction {
        return onView(ofViewPagerTabWithPosition(R.id.user_tabContainer, position))
            .perform(selectViewPagerTabItem())
    }

    infix fun verify(block: Verify.() -> Unit) {
        Verify().apply(block)
    }

    class Verify : ActivityRobot.VerifyRobot, FabVerify by FabVerify.get(fab)
}

fun ofViewPagerTabWithPosition(
    @IdRes tabLayoutId: Int,
    position: Int
): Matcher<View> = object : BoundedMatcher<View, TabLayout.TabView>(TabLayout.TabView::class.java) {
    override fun describeTo(description: Description?) {
        description?.appendText("tab position: $position")
    }

    override fun matchesSafely(item: TabLayout.TabView): Boolean {
        return item.tab?.parent?.id == tabLayoutId
            && item.tab?.position == position
    }
}

fun selectViewPagerTabItem(): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(TabLayout.TabView::class.java)

    override fun getDescription(): String = "click ViewPager tab item"

    override fun perform(uiController: UiController?, view: View?) {
        val v = view as TabLayout.TabView
        val tab = v.tab ?: throw IllegalStateException()
        val position = tab.position
        requireNotNull(tab.parent).setScrollPosition(position, 0f, false)
        click().perform(uiController, view)
    }
}
