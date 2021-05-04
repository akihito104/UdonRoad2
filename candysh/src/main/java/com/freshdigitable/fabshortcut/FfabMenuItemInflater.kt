/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.fabshortcut

import android.content.Context
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Xml
import android.view.MenuInflater
import androidx.annotation.MenuRes
import androidx.core.content.res.getIntOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.content.withStyledAttributes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

internal class FfabMenuItemInflater private constructor(
    private val context: Context,
) {
    companion object {
        internal fun inflate(context: Context, menu: FfabMenu, @MenuRes menuRes: Int) {
            val inflater = FfabMenuItemInflater(context)
            inflater.inflate(menu, menuRes)
        }
    }

    private fun inflate(menu: FfabMenu, menuRes: Int) {
        val menuInflater = MenuInflater(context)
        menuInflater.inflate(menuRes, menu)
        context.resources.getLayout(menuRes).use { parser ->
            val attributeSet = Xml.asAttributeSet(parser)
            parser.parseMenu(attributeSet, menu)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun XmlResourceParser.parseMenu(attributeSet: AttributeSet, menu: FfabMenu) {
        var eventType = eventType
        var tag: String
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                tag = name
                if (tag == "menu") {
                    eventType = next()
                    break
                }
            }
            eventType = next()
        }

        while (eventType != XmlPullParser.END_DOCUMENT) {
            tag = name
            if (eventType != XmlPullParser.START_TAG || tag != "item") {
                eventType = next()
                continue
            }
            readDirection(attributeSet, menu)
            eventType = next()
        }
    }

    private fun readDirection(
        attributeSet: AttributeSet,
        menu: FfabMenu,
    ) {
        context.withStyledAttributes(attributeSet, R.styleable.FlingFABMenu) {
            if (!hasValue(R.styleable.FlingFABMenu_ffab_direction)) {
                return
            }
            val index = getIntOrThrow(R.styleable.FlingFABMenu_ffab_direction)
            val direction = Direction.findByIndex(index)
            if (direction == Direction.UNDEFINED) {
                throw IllegalArgumentException("undefined direction value")
            }

            val id = getResourceIdOrThrow(R.styleable.FlingFABMenu_android_id)
            val item = menu.findItem(id) as FfabMenuItem
            item.direction = direction
        }
    }
}
