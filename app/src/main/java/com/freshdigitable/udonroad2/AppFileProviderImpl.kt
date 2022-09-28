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

package com.freshdigitable.udonroad2

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.freshdigitable.udonroad2.model.app.AppFilePath
import com.freshdigitable.udonroad2.model.app.AppFileProvider
import java.io.File

internal class AppFileProviderImpl : AppFileProvider {
    override fun getFilePathForFile(
        context: Context,
        type: AppFileProvider.FileType,
        fileName: String
    ): AppFilePath {
        val dir = when (type) {
            AppFileProvider.FileType.MEDIA -> {
                val mediaRoot = when (Build.VERSION.SDK_INT) {
                    in (Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.Q) -> {
                        @Suppress("DEPRECATION")
                        context.externalMediaDirs[0]
                    }
                    else -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                }
                File(mediaRoot, "images")
            }
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)
        return AppFilePath(
            file,
            FileProvider.getUriForFile(context, BuildConfig.FILE_AUTHORITY, file)
        )
    }
}
