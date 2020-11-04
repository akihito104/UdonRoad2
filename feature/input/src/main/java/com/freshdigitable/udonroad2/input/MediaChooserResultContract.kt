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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

internal class MediaChooserResultContract : ActivityResultContract<Unit, Collection<Uri>>() {
    private val pictureResultContract = PickPicture.create()
    private val cameraContract = TakePictureContract()

    override fun createIntent(context: Context, input: Unit?): Intent {
        val pickMediaIntent = pictureResultContract.createIntent(context, input)
        val altIntentName = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Intent.EXTRA_ALTERNATE_INTENTS
            else -> Intent.EXTRA_INITIAL_INTENTS
        }
        return Intent.createChooser(pickMediaIntent, "追加する画像...").apply {
            putExtra(altIntentName, arrayOf(cameraContract.createIntent(context, Unit)))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Collection<Uri> {
        val pictureList = pictureResultContract.parseResult(resultCode, intent)
        return if (pictureList.isNotEmpty()) {
            pictureList
        } else {
            cameraContract.parseResult(resultCode, intent)
        }
    }

    fun clear(context: Context) {
        cameraContract.clear(context)
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
                    private val contentContract = ActivityResultContracts.GetMultipleContents()
                    override fun createIntent(context: Context, input: Unit?): Intent {
                        return contentContract.createIntent(context, "image/*")
                    }

                    override fun parseResult(
                        resultCode: Int,
                        intent: Intent?
                    ): Collection<Uri> = contentContract.parseResult(resultCode, intent)
                }
            }
        }
    }
}

private class TakePictureContract : ActivityResultContract<Unit, Collection<Uri>>() {
    private val takePictureContract = ActivityResultContracts.TakePicture()
    private var pictureUri: Uri? = null
    private var cameraAppPackage: String? = null

    override fun createIntent(context: Context, input: Unit?): Intent {
        val uri = createMedia(context)
        pictureUri = uri
        return takePictureContract.createIntent(context, uri).also {
            val component = it.resolveActivity(context.packageManager)
            val packageName = component.packageName
            Timber.tag("TakePictureContract").d("${component?.toShortString()}")
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            cameraAppPackage = packageName
        }
    }

    private fun createMedia(context: Context): Uri {
        val timeStamp = System.currentTimeMillis()
        val filename = "$timeStamp.jpg"
        val mediaRoot = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> context.externalMediaDirs[0]
            else -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        val dir = File(mediaRoot, "images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, filename)
        return FileProvider.getUriForFile(context, "com.freshdigitable.udonroad2", file)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Collection<Uri> {
        return if (takePictureContract.parseResult(resultCode, intent)) {
            listOf(checkNotNull(pictureUri))
        } else {
            emptyList()
        }
    }

    fun clear(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageName = cameraAppPackage ?: return
            val uri = pictureUri ?: return
            context.revokeUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
}
