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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.freshdigitable.udonroad2.model.app.navigation.EventDispatcher
import dagger.android.AndroidInjection
import java.io.File
import javax.inject.Inject

internal class MediaChooserResultContract(
    private val viewModel: TweetInputViewModel
) : ActivityResultContract<Unit, Collection<Uri>>() {
    private val pictureResultContract = PickPicture.create()
    private val cameraContract = TakePictureContract()

    override fun createIntent(context: Context, input: Unit?): Intent {
        val pickMediaIntent = pictureResultContract.createIntent(context, input)
        val altIntentName = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Intent.EXTRA_ALTERNATE_INTENTS
            else -> Intent.EXTRA_INITIAL_INTENTS
        }
        val cameraIntent = cameraContract.createIntent(context, Unit)
        val title = context.getString(R.string.media_chooser_title)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val candidates = context.packageManager.queryIntentActivities(cameraIntent, 0)
                .map { Components.create(it.activityInfo) }
            viewModel.onCameraAppCandidatesQueried(
                candidates, checkNotNull(cameraIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT))
            )
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, MediaChooserBroadcastReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            Intent.createChooser(pickMediaIntent, title, pendingIntent.intentSender)
        } else {
            Intent.createChooser(pickMediaIntent, title)
        }.apply {
            putExtra(altIntentName, arrayOf(cameraIntent))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Collection<Uri> {
        val pictureList = pictureResultContract.parseResult(resultCode, intent)
        return if (pictureList.isNotEmpty()) {
            pictureList
        } else {
            listOf(
                viewModel.media.value ?: emptyList(),
                cameraContract.parseResult(resultCode, intent)
            ).flatten()
        }
    }
}

private abstract class PickPicture : ActivityResultContract<Unit, Collection<Uri>>() {
    companion object {
        fun create(): PickPicture = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                object : PickPicture() {
                    private val openDocContract = ActivityResultContracts.OpenMultipleDocuments()
                    override val contract: ActivityResultContract<*, out Collection<Uri>> =
                        openDocContract

                    override fun createIntent(context: Context, input: Unit?): Intent {
                        return openDocContract.createIntent(context, arrayOf("image/*"))
                            .addCategory(Intent.CATEGORY_OPENABLE)
                    }
                }
            }
            else -> {
                object : PickPicture() {
                    private val contentContract = ActivityResultContracts.GetMultipleContents()
                    override val contract: ActivityResultContract<*, out Collection<Uri>> =
                        contentContract

                    override fun createIntent(context: Context, input: Unit?): Intent {
                        return contentContract.createIntent(context, "image/*")
                    }
                }
            }
        }
    }

    protected abstract val contract: ActivityResultContract<*, out Collection<Uri>>
    override fun parseResult(resultCode: Int, intent: Intent?): Collection<Uri> {
        return contract.parseResult(resultCode, intent)
    }
}

private class TakePictureContract : ActivityResultContract<Unit, Collection<Uri>>() {
    private val takePictureContract = ActivityResultContracts.TakePicture()
    private var pictureUri: Uri? = null

    override fun createIntent(context: Context, input: Unit?): Intent {
        val uri = createMedia(context)
        pictureUri = uri
        return takePictureContract.createIntent(context, uri)
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
}

class MediaChooserBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var eventDispatcher: EventDispatcher

    override fun onReceive(context: Context?, intent: Intent?) {
        AndroidInjection.inject(this, context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val componentName: ComponentName =
                intent?.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT) ?: return
            eventDispatcher.postEvent(CameraApp.Event.Chosen(Components.create(componentName)))
        }
    }
}

private fun Components.Companion.create(info: ActivityInfo): Components {
    return Components(info.packageName, info.name)
}

private fun Components.Companion.create(name: ComponentName): Components {
    return Components(name.packageName, name.className)
}
