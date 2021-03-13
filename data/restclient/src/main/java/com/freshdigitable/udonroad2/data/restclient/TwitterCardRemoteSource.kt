/*
 * Copyright (c) 2021. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.data.restclient

import android.net.Uri
import android.util.Xml
import com.freshdigitable.udonroad2.data.TwitterCardDataSource
import com.freshdigitable.udonroad2.model.TwitterCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.io.Reader
import java.util.Locale
import javax.inject.Inject

internal class TwitterCardRemoteSource @Inject constructor(
    private val httpClient: OkHttpClient
) : TwitterCardDataSource.Remote {
    override fun getTwitterCardSource(url: String): Flow<TwitterCard?> {
        val request = Request.Builder()
            .url(url)
            .build()
        val call = httpClient.newCall(request)
        return flow {
            val item = call.runCatching {
                val response = execute()
                val meta = findMetaTagForCard(requireNotNull(response.body?.charStream()))
                TwitterCardRemote(response.request.url.toString(), meta)
            }
            if (item.isSuccess) emit(item.getOrThrow())
        }
    }

    override suspend fun putTwitterCard(url: String, card: TwitterCard) =
        throw NotImplementedError()

    companion object {
        private const val TAG = "TwitterCardRemoteSource"

        @Throws(XmlPullParserException::class, IOException::class)
        private fun findMetaTagForCard(reader: Reader): Map<Property, String> {
            Timber.tag(TAG).d("findMetaTagForCard: ")
            val xmlPullParser = Xml.newPullParser()
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            xmlPullParser.setFeature(Xml.FEATURE_RELAXED, true)
            xmlPullParser.setInput(reader)

            var eventType = xmlPullParser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG) {
                    eventType = xmlPullParser.nextTag()
                    continue
                }
                val name = xmlPullParser.name
                if (isHeadTag(name)) {
                    Timber.tag(TAG).d("fetch> head:")
                    return readHead(xmlPullParser)
                }
                eventType = xmlPullParser.next()
            }
            return emptyMap()
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun readHead(xpp: XmlPullParser): Map<Property, String> {
            if (xpp.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException()
            }
            if (!isHeadTag(xpp.name)) {
                throw  IllegalStateException()
            }
            val metadata = mutableMapOf<Property, String>()
            var eventType = xpp.nextTag()
            while (eventType != XmlPullParser.END_TAG || !isHeadTag(xpp.name)) {
                if (xpp.eventType != XmlPullParser.START_TAG) {
                    eventType = xpp.next()
                    continue
                }
                if (isMetaTag(xpp.name)) {
                    val property = readMetaProperty(xpp)
                    if (property != Property.UNKNOWN) {
                        val content = readContent(xpp)
                        metadata[property] = content
                    }
                    eventType = xpp.nextTag()
                } else {
                    eventType = xpp.next()
                }
            }
            Timber.tag(TAG).d("readHead: end")
            return metadata
        }

        @Throws(XmlPullParserException::class)
        private fun readMetaProperty(xpp: XmlPullParser): Property {
            if (xpp.eventType != XmlPullParser.START_TAG) {
                throw  IllegalStateException()
            }
            if (!isMetaTag(xpp.name)) {
                throw  IllegalStateException()
            }
            val p = Property.findByString(xpp.getAttributeValue(null, "name"))
            return if (p != Property.UNKNOWN) p else Property.findByString(
                xpp.getAttributeValue(
                    null,
                    "property"
                )
            )
        }

        private fun readContent(xpp: XmlPullParser): String = xpp.getAttributeValue(null, "content")
        private fun isHeadTag(tag: String): Boolean = "head".equals(tag, ignoreCase = true)
        private fun isMetaTag(tag: String): Boolean = "meta".equals(tag, ignoreCase = true)
    }
}

private enum class Property {
    TWITTER_TITLE, TWITTER_IMAGE, OG_TITLE, OG_IMAGE, TWITTER_APP_URL_GOOGLEPLAY, UNKNOWN;

    private fun toAttrString(): String {
        return name.toLowerCase(Locale.ROOT).replace("_", ":")
    }

    companion object {
        fun findByString(property: String): Property {
            for (p in values()) {
                if (p.toAttrString() == property) {
                    return p
                }
            }
            return UNKNOWN
        }
    }
}

private data class TwitterCardRemote(
    override val url: String,
    private val properties: Map<Property, String> = emptyMap(),
) : TwitterCard {
    override val title: String?
        get() = properties[Property.TWITTER_TITLE] ?: properties[Property.OG_TITLE]
    override val displayUrl: String
        get() = requireNotNull(Uri.parse(url).host)
    override val imageUrl: String?
        get() = properties[Property.TWITTER_IMAGE] ?: properties[Property.OG_IMAGE]
    override val appUrl: String?
        get() = properties[Property.TWITTER_APP_URL_GOOGLEPLAY]
}
