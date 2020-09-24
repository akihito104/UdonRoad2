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

import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.media.MediaThumbnailContainer
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher

fun mainList(block: TimelineRobot.() -> Unit = {}): TimelineRobot {
    return TimelineRobot(R.id.main_list).apply(block)
}

class TimelineRobot(
    @IdRes private val listId: Int
) {
    fun clickListItemOf(adapterPosition: Int): ViewInteraction {
        return onListItemOf(adapterPosition).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                withParent(isAssignableFrom(RecyclerView::class.java))

            override fun getDescription(): String = "adapter position: $adapterPosition"

            override fun perform(uiController: UiController?, view: View?) {
                val target = view?.findViewById<View>(R.id.tweetItem_text)
                click().perform(uiController, target)
            }
        })
    }

    fun clickMediaInListItemOf(itemPosition: Int, mediaIndex: Int = 0) {
        onListItemOf(itemPosition).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                withParent(isAssignableFrom(RecyclerView::class.java))

            override fun getDescription(): String =
                "click media(index:$mediaIndex) of item(position:$itemPosition)"

            override fun perform(uiController: UiController?, view: View?) {
                val target =
                    view?.findViewById<MediaThumbnailContainer>(R.id.tweetItem_mediaContainer)
                        ?.getChildAt(mediaIndex)
                click().perform(uiController, target)
            }
        })
    }

    private fun onListItemOf(adapterPosition: Int): ViewInteraction {
        return onView(
            allOf(
                withParent(withId(listId)),
                withRecyclerViewAdapterPosition(adapterPosition)
            )
        )
    }

    infix fun verify(block: Verify.() -> Unit) {
        Verify(this).apply(block)
    }

    class Verify(private val robot: TimelineRobot) {
        fun stateIsSelectedOnItemOf(adapterPosition: Int) {
            robot.onListItemOf(adapterPosition).check(matches(isSelected()))
        }
    }
}

fun withRecyclerViewAdapterPosition(pos: Int): Matcher<View> {
    return RecyclerViewItemAdapterPositionMatcher(pos)
}

private class RecyclerViewItemAdapterPositionMatcher(
    private val adapterPosition: Int
) : BoundedMatcher<View, View>(View::class.java) {
    override fun matchesSafely(item: View): Boolean {
        val recyclerView = item.parent as? RecyclerView ?: return false
        val vh = recyclerView.findContainingViewHolder(item)
        return vh?.adapterPosition == adapterPosition
    }

    override fun describeTo(description: Description?) {
        description?.appendText("adapterPosition: $adapterPosition")
    }
}
