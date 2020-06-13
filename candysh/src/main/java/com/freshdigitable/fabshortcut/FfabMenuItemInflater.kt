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
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Xml
import android.view.MenuInflater
import androidx.annotation.MenuRes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

internal class FfabMenuItemInflater private constructor(
    private val context: Context
) {
    companion object {
        internal fun inflate(context: Context, menu: FfabMenu, @MenuRes menuRes: Int) {
            val inflater = FfabMenuItemInflater(context)
            inflater.inflate(menu, menuRes)
        }
    }

    private val menuInflater: MenuInflater = MenuInflater(context)

    private fun inflate(menu: FfabMenu, menuRes: Int) {
        menuInflater.inflate(menuRes, menu)
        val parser: XmlResourceParser = context.resources.getLayout(menuRes)
        try {
            val attributeSet = Xml.asAttributeSet(parser)
            parseMenu(parser, attributeSet, menu)
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            parser.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseMenu(parser: XmlResourceParser, attributeSet: AttributeSet, menu: FfabMenu) {
        var eventType = parser.eventType
        var tag: String
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                tag = parser.name
                if (tag == "menu") {
                    eventType = parser.next()
                    break
                }
            }
            eventType = parser.next()
        }

        while (eventType != XmlPullParser.END_DOCUMENT) {
            tag = parser.name
            if (eventType != XmlPullParser.START_TAG || tag != "item") {
                eventType = parser.next()
                continue
            }
            findId(parser)?.let { id ->
                val ta = context.obtainStyledAttributes(attributeSet, R.styleable.FlingFABMenu)
                val direction = Direction.findByIndex(
                    ta.getInt(R.styleable.FlingFABMenu_direction, Direction.UNDEFINED.index)
                )
                if (direction == Direction.UNDEFINED) {
                    throw IllegalArgumentException("undefined direction value")
                }
                val item = menu.findItem(id)
                if (item is FfabMenuItem) {
                    item.direction = direction
                }
                ta.recycle()
            }
            eventType = parser.next()
        }
    }

    private fun findId(parser: XmlPullParser): Int? {
        return (0 until parser.attributeCount)
            .find { parser.getAttributeName(it) == "id" }
            ?.let { index ->
                val attributeValue = parser.getAttributeValue(index)
                attributeValue.substring(1)
            }
            ?.takeIf { TextUtils.isDigitsOnly(it) }
            ?.let { Integer.parseInt(it) }
    }
}
