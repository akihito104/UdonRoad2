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

package com.freshdigitable.udonroad2.model.app.navigation

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import java.io.Serializable
import kotlin.properties.ReadOnlyProperty

interface FeedbackMessageDelegate {
    fun dispatchFeedbackMessage(message: FeedbackMessage)
}

interface FeedbackMessage : Serializable {
    @get:StringRes
    val messageRes: Int
    val args: List<Any>? get() = null
}

fun Context.getString(feedback: FeedbackMessage): String {
    return when {
        feedback.args.isNullOrEmpty() -> getString(feedback.messageRes)
        else -> getString(feedback.messageRes, feedback.args)
    }
}

class SnackbarFeedbackMessageDelegate(
    prop: ReadOnlyProperty<Any, View>,
) : FeedbackMessageDelegate {
    private val snackbarContainer: View by prop

    override fun dispatchFeedbackMessage(message: FeedbackMessage) {
        val context = snackbarContainer.context
        Snackbar.make(snackbarContainer, context.getString(message), Snackbar.LENGTH_SHORT).show()
    }
}
