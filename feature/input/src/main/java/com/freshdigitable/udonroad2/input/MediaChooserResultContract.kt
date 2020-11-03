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

package com.freshdigitable.udonroad2.input

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

internal class MediaChooserResultContract : ActivityResultContract<Unit, Collection<Uri>>() {
    private val pictureResultContract = PickPicture.create()
    private val cameraContract = ActivityResultContracts.TakePicture()

    override fun createIntent(context: Context, input: Unit?): Intent {
        val pickMediaIntent = pictureResultContract.createIntent(context, input)
        val altIntentName = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Intent.EXTRA_ALTERNATE_INTENTS
            else -> Intent.EXTRA_INITIAL_INTENTS
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            val timeStamp = System.currentTimeMillis()
            put(MediaStore.Images.Media.TITLE, "$timeStamp.jpg")
            put(MediaStore.Images.Media.DISPLAY_NAME, "$timeStamp.jpg")
        }
        val cameraPicUri = checkNotNull(
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        )
        return Intent.createChooser(pickMediaIntent, "追加する画像...").apply {
            putExtra(altIntentName, arrayOf(cameraContract.createIntent(context, cameraPicUri)))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Collection<Uri> {
        return pictureResultContract.parseResult(resultCode, intent)
    }
}

private abstract class PickPicture : ActivityResultContract<Unit, Collection<Uri>>() {
    companion object {
        fun create(): PickPicture = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                object : PickPicture() {
                    private val openDocContract = ActivityResultContracts.OpenMultipleDocuments()
                    override fun createIntent(context: Context, input: Unit?): Intent {
                        return openDocContract.createIntent(context, arrayOf("image/*"))
                            .addCategory(Intent.CATEGORY_OPENABLE)
                    }

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?
                    ): Collection<Uri> = openDocContract.parseResult(resultCode, intent)
                }
            }
            else -> {
                object : PickPicture() {
                    private val contentContract = ActivityResultContracts.GetContent()
                    override fun createIntent(context: Context, input: Unit?): Intent {
                        return contentContract.createIntent(context, "image/*")
                    }

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?
                    ): Collection<Uri> {
                        return contentContract.parseResult(resultCode, intent)?.let { listOf(it) }
                            ?: emptyList()
                    }
                }
            }
        }
    }
}
