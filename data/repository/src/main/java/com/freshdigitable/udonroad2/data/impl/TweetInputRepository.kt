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

package com.freshdigitable.udonroad2.data.impl

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import com.freshdigitable.udonroad2.data.restclient.TweetApiClient
import com.freshdigitable.udonroad2.model.MediaId
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.tweet.TweetEntity
import com.freshdigitable.udonroad2.model.tweet.TweetId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TweetInputRepository @Inject constructor(
    private val remoteSource: TweetApiClient,
    private val application: Application,
) {
    suspend fun post(
        text: String,
        mediaIds: List<MediaId> = emptyList(),
        replyTo: TweetId? = null
    ): TweetEntity = remoteSource.postTweet(text, mediaIds, replyTo)

    suspend fun uploadMedia(path: AppFilePath): MediaId {
        val file = path.file
        val (filename, inputStream) = when {
            file != null -> getStreams(file)
            else -> getStreams(path.uri)
        }
        return remoteSource.uploadMedia(filename, inputStream)
    }

    private suspend fun getStreams(
        file: File
    ): Pair<String, InputStream> = withContext(Dispatchers.IO) {
        file.name to file.inputStream()
    }

    private suspend fun getStreams(
        uri: Uri
    ): Pair<String, InputStream> = withContext(Dispatchers.IO) {
        val filename = application.contentResolver.query(
            uri, PROJECTION_DISPLAY_NAME, null, null, null
        ).use {
            when {
                it?.moveToFirst() == true -> it.getString(0)
                else -> throw IllegalStateException()
            }
        }

        val fileDescriptor = application.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException()
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

        filename to inputStream
    }

    companion object {
        private val PROJECTION_DISPLAY_NAME = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
    }
}
